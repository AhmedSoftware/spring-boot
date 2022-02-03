/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Date;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnBean @ConditionalOnBean}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Uladzislau Seuruk
 */
class ConditionalOnBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void testNameOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testNameAndTypeOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameAndTypeConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void testNameOnBeanConditionReverseOrder() {
		// Ideally this should be true
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class, FooConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void testClassOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testClassOnBeanClassNameCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassNameConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testOnBeanConditionWithXml() {
		this.contextRunner.withUserConfiguration(XmlConfiguration.class, OnBeanNameConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testOnBeanConditionWithCombinedXml() {
		// Ideally this should be true
		this.contextRunner.withUserConfiguration(CombinedXmlConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void testAnnotationOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	void testOnMissingBeanType() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanMissingClassConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	void withPropertyPlaceholderClassName() {
		this.contextRunner
				.withUserConfiguration(PropertySourcesPlaceholderConfigurer.class,
						WithPropertyPlaceholderClassName.class, OnBeanClassConfiguration.class)
				.withPropertyValues("mybeanclass=java.lang.String")
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.contextRunner
				.withUserConfiguration(FactoryBeanConfiguration.class, OnAnnotationWithFactoryBeanConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("bar");
					assertThat(context).hasSingleBean(ExampleBean.class);
				});
	}

	private void hasBarBean(AssertableApplicationContext context) {
		assertThat(context).hasBean("bar");
		assertThat(context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	void onBeanConditionOutputShouldNotContainConditionalOnMissingBeanClassInMessage() {
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class).run((context) -> {
			Collection<ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
					.get(context.getSourceApplicationContext().getBeanFactory()).getConditionAndOutcomesBySource()
					.values();
			String message = conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
			assertThat(message).doesNotContain("@ConditionalOnMissingBean");
		});
	}

	@Test
	void conditionEvaluationConsidersChangeInTypeWhenBeanIsOverridden() {
		this.contextRunner.withAllowBeanDefinitionOverriding(true).withUserConfiguration(OriginalDefinition.class,
				OverridingDefinition.class, ConsumingConfiguration.class).run((context) -> {
					assertThat(context).hasBean("testBean");
					assertThat(context).hasSingleBean(Integer.class);
					assertThat(context).doesNotHaveBean(ConsumingConfiguration.class);
				});
	}

	@Test
	void parameterizedContainerWhenValueIsOfMissingBeanDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithoutCustomConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("otherExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfExistingBeanMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class, ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfMissingBeanRegistrationDoesNotMatch() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithoutCustomContainerConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("otherExampleBean")));
	}

	@Test
	void parameterizedContainerWhenValueIsOfExistingBeanRegistrationMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
						ParameterizedConditionWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnTypeIsOfExistingBeanMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						ParameterizedConditionWithReturnTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnTypeIsOfExistingBeanRegistrationMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
						ParameterizedConditionWithReturnTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						ParameterizedConditionWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void parameterizedContainerWhenReturnRegistrationTypeIsOfExistingBeanRegistrationMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomContainerConfig.class,
						ParameterizedConditionWithReturnRegistrationTypeConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "conditionalCustomExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class, GenericWithStringTypeArgumentsConfig.class,
						GenericWithIntegerTypeArgumentsConfig.class)
				.run((context) -> assertThat(context).satisfies(
						exampleBeanRequirement("customExampleBean", "genericStringTypeArgumentsExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentNameNotMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithIntegerConfig.class, GenericWithStringTypeArgumentNamesConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericIntegerExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentNameMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringConfig.class, GenericWithStringTypeArgumentNamesConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("genericStringExampleBean", "genericStringNameExampleBean")));
	}

	@Test
	void genericWhenTypeArgumentWithValueMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringConfig.class, TypeArgumentsConditionWithValueConfig.class)
				.run((context) -> assertThat(context).satisfies(
						exampleBeanRequirement("genericStringExampleBean", "genericStringWithValueExampleBean")));
	}

	@Test
	void genericWithValueWhenSubclassTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class, TypeArgumentsConditionWithValueConfig.class)
				.run((context) -> assertThat(context)
						.satisfies(exampleBeanRequirement("customExampleBean", "genericStringWithValueExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentNotMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithIntegerConfig.class,
						TypeArgumentsConditionWithParameterizedContainerConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericIntegerExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringConfig.class,
						TypeArgumentsConditionWithParameterizedContainerConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericStringExampleBean",
						"parameterizedContainerGenericExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWhenSubclassTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						TypeArgumentsConditionWithParameterizedContainerConfig.class)
				.run((context) -> assertThat(context).satisfies(
						exampleBeanRequirement("customExampleBean", "parameterizedContainerGenericExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWithValueWhenTypeArgumentNotMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithIntegerConfig.class,
						TypeArgumentsConditionWithParameterizedContainerAndValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericIntegerExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWithValueWhenTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(GenericWithStringConfig.class,
						TypeArgumentsConditionWithParameterizedContainerAndValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("genericStringExampleBean",
						"parameterizedContainerGenericWithValueExampleBean")));
	}

	@Test
	void parameterizedContainerGenericWithValueWhenSubclassTypeArgumentMatches() {
		this.contextRunner
				.withUserConfiguration(ParameterizedWithCustomConfig.class,
						TypeArgumentsConditionWithParameterizedContainerAndValueConfig.class)
				.run((context) -> assertThat(context).satisfies(exampleBeanRequirement("customExampleBean",
						"parameterizedContainerGenericWithValueExampleBean")));
	}

	private Consumer<ConfigurableApplicationContext> exampleBeanRequirement(String... names) {
		return (context) -> {
			String[] beans = context.getBeanNamesForType(ExampleBean.class);
			String[] containers = context.getBeanNamesForType(TestParameterizedContainer.class);
			assertThat(StringUtils.concatenateStringArrays(beans, containers)).containsOnly(names);
		};
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = "foo")
	static class OnBeanNameConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(name = "foo", value = Date.class)
	static class OnBeanNameAndTypeConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(annotation = EnableScheduling.class)
	static class OnAnnotationConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(String.class)
	static class OnBeanClassConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(type = "java.lang.String")
	static class OnBeanClassNameConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(type = "some.type.Missing")
	static class OnBeanMissingClassConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	static class FooConfiguration {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	static class XmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	@Import(OnBeanNameConfiguration.class)
	static class CombinedXmlConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(WithPropertyPlaceholderClassNameRegistrar.class)
	static class WithPropertyPlaceholderClassName {

	}

	@Configuration(proxyBeanMethods = false)
	static class FactoryBeanConfiguration {

		@Bean
		ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(annotation = TestAnnotation.class)
	static class OnAnnotationWithFactoryBeanConfiguration {

		@Bean
		String bar() {
			return "bar";
		}

	}

	static class WithPropertyPlaceholderClassNameRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			RootBeanDefinition bd = new RootBeanDefinition();
			bd.setBeanClassName("${mybeanclass}");
			registry.registerBeanDefinition("mybean", bd);
		}

	}

	static class ExampleFactoryBean implements FactoryBean<ExampleBean<String>> {

		@Override
		public ExampleBean<String> getObject() {
			return new ExampleBean<>("fromFactory");
		}

		@Override
		public Class<?> getObjectType() {
			return ExampleBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OriginalDefinition {

		@Bean
		String testBean() {
			return "test";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(String.class)
	static class OverridingDefinition {

		@Bean
		Integer testBean() {
			return 1;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(String.class)
	static class ConsumingConfiguration {

		ConsumingConfiguration(String testBean) {
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomConfig {

		@Bean
		CustomExampleBean customExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithoutCustomConfig {

		@Bean
		OtherExampleBean otherExampleBean() {
			return new OtherExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithoutCustomContainerConfig {

		@Bean
		TestParameterizedContainer<OtherExampleBean> otherExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedWithCustomContainerConfig {

		@Bean
		TestParameterizedContainer<CustomExampleBean> customExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithValueConfig {

		@Bean
		@ConditionalOnBean(value = CustomExampleBean.class, parameterizedContainer = TestParameterizedContainer.class)
		CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnTypeConfig {

		@Bean
		@ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
		CustomExampleBean conditionalCustomExampleBean() {
			return new CustomExampleBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterizedConditionWithReturnRegistrationTypeConfig {

		@Bean
		@ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
		TestParameterizedContainer<CustomExampleBean> conditionalCustomExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithStringConfig {

		@Bean
		ExampleBean<String> genericStringExampleBean() {
			return new ExampleBean<>("genericStringExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithStringTypeArgumentsConfig {

		@Bean
		@ConditionalOnBean
		ExampleBean<String> genericStringTypeArgumentsExampleBean() {
			return new ExampleBean<>("genericStringTypeArgumentsExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithIntegerConfig {

		@Bean
		ExampleBean<Integer> genericIntegerExampleBean() {
			return new ExampleBean<>(1_000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithIntegerTypeArgumentsConfig {

		@Bean
		@ConditionalOnBean
		ExampleBean<Integer> genericIntegerTypeArgumentsExampleBean() {
			return new ExampleBean<>(1_000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithValueConfig {

		@Bean
		@ConditionalOnBean(value = ExampleBean.class, typeArguments = String.class)
		ExampleBean<String> genericStringWithValueExampleBean() {
			return new ExampleBean<>("genericStringWithValueExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GenericWithStringTypeArgumentNamesConfig {

		@Bean
		@ConditionalOnBean(value = ExampleBean.class, typeArgumentNames = "java.lang.String")
		ExampleBean<String> genericStringNameExampleBean() {
			return new ExampleBean<>("genericStringNameExampleBean");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithParameterizedContainerConfig {

		@Bean
		@ConditionalOnBean(parameterizedContainer = TestParameterizedContainer.class)
		TestParameterizedContainer<ExampleBean<String>> parameterizedContainerGenericExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TypeArgumentsConditionWithParameterizedContainerAndValueConfig {

		@Bean
		@ConditionalOnBean(value = ExampleBean.class, parameterizedContainer = TestParameterizedContainer.class,
				typeArguments = String.class)
		TestParameterizedContainer<ExampleBean<String>> parameterizedContainerGenericWithValueExampleBean() {
			return new TestParameterizedContainer<>();
		}

	}

	@TestAnnotation
	static class ExampleBean<T> {

		private final T value;

		ExampleBean(T value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return String.valueOf(this.value);
		}

	}

	static class CustomExampleBean extends ExampleBean<String> {

		CustomExampleBean() {
			super("custom subclass");
		}

	}

	static class OtherExampleBean extends ExampleBean<String> {

		OtherExampleBean() {
			super("other subclass");
		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface TestAnnotation {

	}

}
