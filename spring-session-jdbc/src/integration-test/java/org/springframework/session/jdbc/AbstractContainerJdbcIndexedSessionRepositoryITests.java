/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.session.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

/**
 * Base class for Testcontainers based {@link JdbcIndexedSessionRepository} integration
 * tests.
 *
 * @author Vedran Pavic
 */
abstract class AbstractContainerJdbcIndexedSessionRepositoryITests extends AbstractJdbcIndexedSessionRepositoryITests {

	static class BaseContainerConfig extends BaseConfig {

		@Bean
		HikariDataSource dataSource(JdbcDatabaseContainer databaseContainer) {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setJdbcUrl(databaseContainer.getJdbcUrl());
			dataSource.setUsername(databaseContainer.getUsername());
			dataSource.setPassword(databaseContainer.getPassword());
			return dataSource;
		}

		@Bean
		DataSourceInitializer dataSourceInitializer(DataSource dataSource, DatabasePopulator databasePopulator) {
			DataSourceInitializer initializer = new DataSourceInitializer();
			initializer.setDataSource(dataSource);
			initializer.setDatabasePopulator(databasePopulator);
			return initializer;
		}

	}

}
