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

package org.springframework.boot.gradle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

/**
 * @author Andy Wilkinson
 */
public class ProjectCreator {

	private String gradleVersion;

	public ProjectCreator() {
		this("1.12");
	}

	public ProjectCreator(String gradleVersion) {
		this.gradleVersion = gradleVersion;
	}

	public ProjectConnection createProject(String name) throws IOException {
		File projectDirectory = new File("target/" + name);
		projectDirectory.mkdirs();

		File gradleScript = new File(projectDirectory, "build.gradle");
		writeGradleProperties(projectDirectory);
        
		if (new File("src/test/resources", name).isDirectory()) {
			FileSystemUtils.copyRecursively(new File("src/test/resources", name),
					projectDirectory);
		}
		else {
			FileCopyUtils.copy(new File("src/test/resources/" + name + ".gradle"),
					gradleScript);
		}

		GradleConnector gradleConnector = GradleConnector.newConnector();
		gradleConnector.useGradleVersion(this.gradleVersion);

		((DefaultGradleConnector) gradleConnector).embedded(true);
		return gradleConnector.forProjectDirectory(projectDirectory).connect();
	}

	private void writeGradleProperties(File projectDirectory) throws IOException {
		File gradleProperties = new File(projectDirectory, "gradle.properties");
        BufferedWriter writer = new BufferedWriter(new FileWriter(gradleProperties));
        writeProperty(writer, "http.proxyHost");
        writeProperty(writer, "https.proxyHost");
        writeProperty(writer, "http.proxyPort");
        writeProperty(writer, "https.proxyPort");
        writer.close();		
	}

	private void writeProperty(BufferedWriter writer, String name) throws IOException {
		String value = System.getProperty(name);
        if (value != null) {
        	writer.write("systemProp." + name + "=" + value + "\n");
        }
	}
}
