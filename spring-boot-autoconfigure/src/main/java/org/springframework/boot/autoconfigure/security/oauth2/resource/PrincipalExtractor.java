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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Map;

/**
 * Strategy used by {@link UserInfoTokenServices} to extract the principal from the
 * resource server's response.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
@FunctionalInterface
public interface PrincipalExtractor {

	/**
	 * Extract the principal that should be used for the token.
	 * @param map the source map
	 * @return the extracted principal or {@code null}
	 */
	Object extractPrincipal(Map<String, Object> map);

}
