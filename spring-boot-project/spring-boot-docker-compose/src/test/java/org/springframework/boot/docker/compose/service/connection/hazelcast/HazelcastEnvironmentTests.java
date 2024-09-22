/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.hazelcast;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastEnvironment}.
 *
 * @author Dmytro Nosan
 */
class HazelcastEnvironmentTests {

	@Test
	void getClusterNameWhenHasNoHzClusterNameSet() {
		HazelcastEnvironment environment = new HazelcastEnvironment(Collections.emptyMap());
		assertThat(environment.getClusterName()).isEmpty();
	}

	@Test
	void getClusterNameWhenHzClusterNameSet() {
		HazelcastEnvironment environment = new HazelcastEnvironment(Map.of("HZ_CLUSTERNAME", "spring-boot"));
		assertThat(environment.getClusterName()).isNotEmpty().hasValue("spring-boot");
	}

}
