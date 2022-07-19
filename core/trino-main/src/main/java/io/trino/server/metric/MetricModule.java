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

import com.google.inject.Binder;
import com.google.inject.Scopes;
import io.airlift.configuration.AbstractConfigurationAwareModule;
import io.trino.server.metric.impl.ClusterResourceMetricSource;
import io.trino.server.metric.impl.NodeMemoryMetricSource;
import io.trino.server.metric.impl.ResourceGroupMetricSource;

import static io.airlift.jaxrs.JaxrsBinder.jaxrsBinder;
import static io.airlift.json.JsonCodecBinder.jsonCodecBinder;

public class MetricModule
        extends AbstractConfigurationAwareModule
{
    @Override
    protected void setup(Binder binder)
    {
        binder.bind(MetricRegistry.class).in(Scopes.SINGLETON);
        binder.bind(ClusterResourceMetricSource.class).in(Scopes.SINGLETON);
        binder.bind(NodeMemoryMetricSource.class).in(Scopes.SINGLETON);
        binder.bind(ResourceGroupMetricSource.class).in(Scopes.SINGLETON);
        binder.bind(MetricCollector.class).in(Scopes.SINGLETON);

        jsonCodecBinder(binder).bindJsonCodec(MetricSet.class);
        jaxrsBinder(binder).bind(MetricResource.class);
    }
}
