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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Integration tests for {@link JdbcIndexedSessionRepository} using Oracle database.
 * <p>
 * This test is conditional on Testcontainers property {@code oracle.container.image}
 * being set.
 *
 * @author Vedran Pavic
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
class OracleJdbcIndexedSessionRepositoryITests extends AbstractContainerJdbcIndexedSessionRepositoryITests {

	@BeforeAll
	static void setUpClass() {
		Assumptions.assumeTrue(
				TestcontainersConfiguration.getInstance().getProperties().containsKey("oracle.container.image"),
				"Testcontainers property `oracle.container.image` is set");
	}

	@Configuration
	static class Config extends BaseContainerConfig {

		@Bean
		OracleContainer databaseContainer() {
			OracleContainer databaseContainer = DatabaseContainers.oracleXe();
			databaseContainer.start();
			return databaseContainer;
		}

		@Bean
		ResourceDatabasePopulator databasePopulator() {
			return DatabasePopulators.oracle();
		}

	}

}
