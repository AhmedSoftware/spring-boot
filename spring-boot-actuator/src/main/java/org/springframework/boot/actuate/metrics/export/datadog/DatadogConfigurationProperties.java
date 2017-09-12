/**
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
package org.springframework.boot.actuate.metrics.export.datadog;

import org.springframework.boot.actuate.metrics.export.StepRegistryConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.micrometer.datadog.DatadogConfig;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
@ConfigurationProperties(prefix = "metrics.datadog")
public class DatadogConfigurationProperties extends StepRegistryConfigurationProperties implements DatadogConfig {
	public DatadogConfigurationProperties() {
		set("apiKey", "dummyKey"); // FIXME otherwise tests fail
	}

    public void setApiKey(String apiKey) {
        set("apiKey", apiKey);
    }

    public void setHostTag(String hostTag) {
        set("hostTag", hostTag);
    }

    @Override
    public String prefix() {
        return "metrics.datadog";
    }
}
