/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.ProxyDataSourceAvailableCondition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * Actual DataSource configurations imported by {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Arthur Gavlyukovskiy
 */
abstract class DataSourceConfiguration {

	@SuppressWarnings("unchecked")
	protected <T extends DataSource> T createDataSource(DataSourceProperties properties,
			Class<T> type) {
		return (T) properties.initializeDataSourceBuilder().type(type).build();
	}

	/**
	 * Proxy over Tomcat Pool DataSource configuration.
	 */
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	@Conditional(ProxyDataSourceAvailableCondition.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource", matchIfMissing = true)
	static class ProxyOverTomcat extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.tomcat")
		public DataSource dataSource(DataSourceProperties properties) {
			DataSource dataSource = properties.initializeDataSourceBuilder().build();
			DataSource realDataSource = ProxyDataSourceUtil.tryFindRealDataSource(dataSource);
			if (realDataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
				org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource =
						(org.apache.tomcat.jdbc.pool.DataSource) realDataSource;
				DatabaseDriver databaseDriver = DatabaseDriver
						.fromJdbcUrl(properties.determineUrl());
				String validationQuery = databaseDriver.getValidationQuery();
				if (validationQuery != null) {
					tomcatDataSource.setTestOnBorrow(true);
					tomcatDataSource.setValidationQuery(validationQuery);
				}
			}
			return dataSource;
		}

	}

	/**
	 * Tomcat Pool DataSource configuration.
	 */
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource", matchIfMissing = true)
	static class Tomcat extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.tomcat")
		public org.apache.tomcat.jdbc.pool.DataSource dataSource(
				DataSourceProperties properties) {
			org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(
					properties, org.apache.tomcat.jdbc.pool.DataSource.class);
			DatabaseDriver databaseDriver = DatabaseDriver
					.fromJdbcUrl(properties.determineUrl());
			String validationQuery = databaseDriver.getValidationQuery();
			if (validationQuery != null) {
				dataSource.setTestOnBorrow(true);
				dataSource.setValidationQuery(validationQuery);
			}
			return dataSource;
		}

	}

	/**
	 * Proxy over Hikari DataSource configuration.
	 */
	@ConditionalOnClass(HikariDataSource.class)
	@Conditional(ProxyDataSourceAvailableCondition.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
	static class ProxyOverHikari extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.hikari")
		public DataSource dataSource(DataSourceProperties properties) {
			return properties.initializeDataSourceBuilder().build();
		}

	}

	/**
	 * Hikari DataSource configuration.
	 */
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource", matchIfMissing = true)
	static class Hikari extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.hikari")
		public HikariDataSource dataSource(DataSourceProperties properties) {
			return createDataSource(properties, HikariDataSource.class);
		}

	}

	/**
	 * Proxy over DBCP DataSource configuration.
	 *
	 * @deprecated as of 1.5 in favor of DBCP2
	 */
	@ConditionalOnClass(org.apache.commons.dbcp.BasicDataSource.class)
	@Conditional(ProxyDataSourceAvailableCondition.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp.BasicDataSource", matchIfMissing = true)
	@Deprecated
	static class ProxyOverDbcp extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp")
		public DataSource dataSource(DataSourceProperties properties) {
			DataSource dataSource = properties.initializeDataSourceBuilder().build();
			DataSource realDataSource = ProxyDataSourceUtil.tryFindRealDataSource(dataSource);
			if (realDataSource instanceof org.apache.commons.dbcp.BasicDataSource) {
				org.apache.commons.dbcp.BasicDataSource basicDataSource =
						(org.apache.commons.dbcp.BasicDataSource) realDataSource;
				DatabaseDriver databaseDriver = DatabaseDriver
						.fromJdbcUrl(properties.determineUrl());
				String validationQuery = databaseDriver.getValidationQuery();
				if (validationQuery != null) {
					basicDataSource.setTestOnBorrow(true);
					basicDataSource.setValidationQuery(validationQuery);
				}
				return dataSource;
			}
			return dataSource;
		}

	}

	/**
	 * DBCP DataSource configuration.
	 *
	 * @deprecated as of 1.5 in favor of DBCP2
	 */
	@ConditionalOnClass(org.apache.commons.dbcp.BasicDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp.BasicDataSource", matchIfMissing = true)
	@Deprecated
	static class Dbcp extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp")
		public org.apache.commons.dbcp.BasicDataSource dataSource(
				DataSourceProperties properties) {
			org.apache.commons.dbcp.BasicDataSource dataSource = createDataSource(
					properties, org.apache.commons.dbcp.BasicDataSource.class);
			DatabaseDriver databaseDriver = DatabaseDriver
					.fromJdbcUrl(properties.determineUrl());
			String validationQuery = databaseDriver.getValidationQuery();
			if (validationQuery != null) {
				dataSource.setTestOnBorrow(true);
				dataSource.setValidationQuery(validationQuery);
			}
			return dataSource;
		}

	}

	/**
	 * Proxy over DBCP2 DataSource configuration.
	 */
	@ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
	@Conditional(ProxyDataSourceAvailableCondition.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource", matchIfMissing = true)
	static class ProxyOverDbcp2 extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp2")
		public DataSource dataSource(DataSourceProperties properties) {
			return properties.initializeDataSourceBuilder().build();
		}

	}

	/**
	 * DBCP2 DataSource configuration.
	 */
	@ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource", matchIfMissing = true)
	static class Dbcp2 extends DataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp2")
		public org.apache.commons.dbcp2.BasicDataSource dataSource(
				DataSourceProperties properties) {
			return createDataSource(properties,
					org.apache.commons.dbcp2.BasicDataSource.class);
		}

	}

	/**
	 * Generic DataSource configuration.
	 */
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type")
	static class Generic {

		@Bean
		public DataSource dataSource(DataSourceProperties properties) {
			return properties.initializeDataSourceBuilder().build();
		}

	}

}
