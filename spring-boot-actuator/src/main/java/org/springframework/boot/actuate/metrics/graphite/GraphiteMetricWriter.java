/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.graphite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A {@link MetricWriter} that pushes data to Graphite.
 *
 * @author Mark Sailes
 */
public class GraphiteMetricWriter implements MetricWriter {

    private static final Log logger = LogFactory.getLog(GraphiteMetricWriter.class);

    private final String prefix;
    private final String host;
    private final int port;

    public GraphiteMetricWriter(String host, int port) {
        this(null, host, port);
    }

    public GraphiteMetricWriter(String prefix, String host, int port) {
        this.prefix = trimPrefix(prefix);
        this.host = host;
        this.port = port;
    }

    private static String trimPrefix(String prefix) {
        String trimmedPrefix = StringUtils.hasText(prefix) ? prefix : null;
        while (trimmedPrefix != null && trimmedPrefix.endsWith(".")) {
            trimmedPrefix = trimmedPrefix.substring(0, trimmedPrefix.length() - 1);
        }

        return trimmedPrefix;
    }

    @Override
    public void increment(Delta<?> delta) {
        sendMetric(delta);
    }

    @Override
    public void reset(String s) {
        // Not implemented
    }

    @Override
    public void set(Metric<?> metric) {
        sendMetric(metric);
    }

    private void sendMetric(Metric<?> metric) {
        String fullMetrix = this.prefix + "." + metric.getName();

        try (Socket socket = new Socket(this.host, this.port);
             OutputStream stream = socket.getOutputStream()) {
            String payload = String.format("%s %d %d%n", fullMetrix, metric.getValue().intValue(), metric.getTimestamp().getTime() / 1000);
            stream.write(payload.getBytes());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}