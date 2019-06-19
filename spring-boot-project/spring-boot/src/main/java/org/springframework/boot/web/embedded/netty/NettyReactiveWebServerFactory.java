/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.netty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link NettyWebServer}s.
 * <p>
 * If the factory port is not 0, it will override the tcp server's port
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class NettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private final TcpServer tcpServer;
	private List<NettyServerCustomizer> serverCustomizers = new ArrayList<>();
	private List<NettyRouteProvider> routeProviders = new ArrayList<>();
	private Duration lifecycleTimeout;
	private boolean useForwardHeaders;

	public NettyReactiveWebServerFactory(TcpServer tcpServer) {
		super(0);
		this.tcpServer = tcpServer;
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		HttpServer httpServer = createHttpServer();
		ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
		NettyWebServer webServer = new NettyWebServer(httpServer, handlerAdapter, this.lifecycleTimeout);
		webServer.setRouteProviders(this.routeProviders);
		return webServer;
	}

	/**
	 * Returns a mutable collection of the {@link NettyServerCustomizer}s that will be
	 * applied to the Netty server builder.
	 * @return the customizers that will be applied
	 */
	public Collection<NettyServerCustomizer> getServerCustomizers() {
		return this.serverCustomizers;
	}

	/**
	 * Set {@link NettyServerCustomizer}s that should be applied to the Netty server
	 * builder. Calling this method will replace any existing customizers.
	 * @param serverCustomizers the customizers to set
	 */
	public void setServerCustomizers(Collection<? extends NettyServerCustomizer> serverCustomizers) {
		Assert.notNull(serverCustomizers, "ServerCustomizers must not be null");
		this.serverCustomizers = new ArrayList<>(serverCustomizers);
	}

	/**
	 * Add {@link NettyServerCustomizer}s that should applied while building the server.
	 * @param serverCustomizers the customizers to add
	 */
	public void addServerCustomizers(NettyServerCustomizer... serverCustomizers) {
		Assert.notNull(serverCustomizers, "ServerCustomizer must not be null");
		this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
	}

	/**
	 * Add {@link NettyRouteProvider}s that should be applied, in order, before the the
	 * handler for the Spring application.
	 * @param routeProviders the route providers to add
	 */
	public void addRouteProviders(NettyRouteProvider... routeProviders) {
		Assert.notNull(routeProviders, "NettyRouteProvider must not be null");
		this.routeProviders.addAll(Arrays.asList(routeProviders));
	}

	/**
	 * Set the maximum amount of time that should be waited when starting or stopping the
	 * server.
	 * @param lifecycleTimeout the lifecycle timeout
	 */
	public void setLifecycleTimeout(Duration lifecycleTimeout) {
		this.lifecycleTimeout = lifecycleTimeout;
	}

	/**
	 * Set if x-forward-* headers should be processed.
	 * @param useForwardHeaders if x-forward headers should be used
	 * @since 2.1.0
	 */
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	private HttpServer createHttpServer() {
		HttpServer server = HttpServer.from(this.tcpServer);
		if (getPort() != 0) {
			server = server.tcpConfiguration((TcpServer tcp) -> tcp.port(getPort()));
		}
		if (getSsl() != null && getSsl().isEnabled()) {
			SslServerCustomizer sslServerCustomizer = new SslServerCustomizer(getSsl(), getHttp2(),
					getSslStoreProvider());
			server = sslServerCustomizer.apply(server);
		}
		if (getCompression() != null && getCompression().getEnabled()) {
			CompressionCustomizer compressionCustomizer = new CompressionCustomizer(getCompression());
			server = compressionCustomizer.apply(server);
		}
		server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
		return applyCustomizers(server);
	}

	private HttpProtocol[] listProtocols() {
		if (getHttp2() != null && getHttp2().isEnabled()) {
			if (getSsl() != null && getSsl().isEnabled()) {
				return new HttpProtocol[] { HttpProtocol.H2, HttpProtocol.HTTP11 };
			}
			else {
				return new HttpProtocol[] { HttpProtocol.H2C, HttpProtocol.HTTP11 };
			}
		}
		return new HttpProtocol[] { HttpProtocol.HTTP11 };
	}

	private HttpServer applyCustomizers(HttpServer server) {
		for (NettyServerCustomizer customizer : this.serverCustomizers) {
			server = customizer.apply(server);
		}
		return server;
	}

}
