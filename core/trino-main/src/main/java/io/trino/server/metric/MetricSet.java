/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.server.metric;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.util.Map;

public class MetricSet
{
    private static final String COMPONENT = "trino";

    private String component = COMPONENT;
    private Map<String, String> metadata;
    private Map<String, Number> indicators;
    private long timestamp;

    public MetricSet(Map<String, String> metadata,
                     Map<String, Number> indicators)
    {
        this(metadata, indicators, -1);
    }

    public MetricSet(Map<String, String> metadata,
                     Map<String, Number> indicators,
                     long timestamp)
    {
        this.metadata = metadata;
        this.indicators = indicators;
        this.timestamp = timestamp;
    }

    @JsonGetter
    public String getComponent()
    {
        return component;
    }

    @JsonGetter("meta")
    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    @JsonGetter
    public Map<String, Number> getIndicators()
    {
        return indicators;
    }

    @JsonGetter
    public long getTimestamp()
    {
        return timestamp;
    }

    public static boolean isCountMetric(String metricName)
    {
        return metricName.endsWith(".count");
    }
}
