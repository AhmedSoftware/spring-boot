/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.Collections;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

class SsoSecurityConfigurer {

	private BeanFactory beanFactory;

	SsoSecurityConfigurer(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public void configure(HttpSecurity http) throws Exception {
		OAuth2SsoProperties sso = this.beanFactory.getBean(OAuth2SsoProperties.class);
		// Delay the processing of the filter until we know the
		// SessionAuthenticationStrategy is available:
		http.apply(new OAuth2ClientAuthenticationConfigurer(oauth2SsoFilter(sso)));
		addAuthenticationEntryPoint(http, sso);
	}

	private void addAuthenticationEntryPoint(HttpSecurity http, OAuth2SsoProperties sso)
			throws Exception {
		ExceptionHandlingConfigurer<HttpSecurity> exceptions = http.exceptionHandling();
		ContentNegotiationStrategy contentNegotiationStrategy = http
				.getSharedObject(ContentNegotiationStrategy.class);
		if (contentNegotiationStrategy == null) {
			contentNegotiationStrategy = new HeaderContentNegotiationStrategy();
		}
		MediaTypeRequestMatcher preferredMatcher = new MediaTypeRequestMatcher(
				contentNegotiationStrategy, MediaType.APPLICATION_XHTML_XML,
				new MediaType("image", "*"), MediaType.TEXT_HTML, MediaType.TEXT_PLAIN);
		preferredMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));
		exceptions.defaultAuthenticationEntryPointFor(
				new LoginUrlAuthenticationEntryPoint(sso.getLoginPath()),
				preferredMatcher);
		// When multiple entry points are provided the default is the first one
		exceptions.defaultAuthenticationEntryPointFor(
				new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
				new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));
	}

	private OAuth2ClientAuthenticationProcessingFilter oauth2SsoFilter(
			OAuth2SsoProperties sso) {
		OAuth2RestOperations restTemplate = this.beanFactory
				.getBean(OAuth2RestOperations.class);
		ResourceServerTokenServices tokenServices = this.beanFactory
				.getBean(ResourceServerTokenServices.class);
		OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
				sso.getLoginPath());
		filter.setRestTemplate(restTemplate);
		filter.setTokenServices(tokenServices);
		filter.setAuthenticationSuccessHandler(successHandler(sso));
		filter.setAuthenticationFailureHandler(failureHandler(sso));
		return filter;
	}

	private SavedRequestAwareAuthenticationSuccessHandler successHandler(OAuth2SsoProperties sso) {
		SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
		handler.setAlwaysUseDefaultTargetUrl(sso.isSuccessAlwaysUseDefaultTargetUrl());
		handler.setDefaultTargetUrl(sso.getSuccesDefaultTargetUrl());
		handler.setUseReferer(sso.isSuccessUseReferer());
		handler.setTargetUrlParameter(sso.getSuccessTargetUrlParameter());

		DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
		redirectStrategy.setContextRelative(sso.isSuccessRedirectContextRelative());
		handler.setRedirectStrategy(redirectStrategy);

		return handler;
	}

	private SimpleUrlAuthenticationFailureHandler failureHandler(OAuth2SsoProperties sso) {
		SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler();
		handler.setAllowSessionCreation(sso.isFailureAllowSessionCreation());
		handler.setDefaultFailureUrl(sso.getFailureDefaultTargetUrl());
		handler.setUseForward(sso.isFailureForwardToDestination());

		DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
		redirectStrategy.setContextRelative(sso.isFailureRedirectContextRelative());
		handler.setRedirectStrategy(redirectStrategy);

		return handler;
	}

	private static class OAuth2ClientAuthenticationConfigurer
			extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

		private OAuth2ClientAuthenticationProcessingFilter filter;

		OAuth2ClientAuthenticationConfigurer(
				OAuth2ClientAuthenticationProcessingFilter filter) {
			this.filter = filter;
		}

		@Override
		public void configure(HttpSecurity builder) throws Exception {
			OAuth2ClientAuthenticationProcessingFilter ssoFilter = this.filter;
			ssoFilter.setSessionAuthenticationStrategy(
					builder.getSharedObject(SessionAuthenticationStrategy.class));
			builder.addFilterAfter(ssoFilter,
					AbstractPreAuthenticatedProcessingFilter.class);
		}

	}

}
