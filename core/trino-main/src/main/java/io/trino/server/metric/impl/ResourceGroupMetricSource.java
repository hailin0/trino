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
package io.trino.server.metric.impl;

import io.trino.execution.resourcegroups.InternalResourceGroupManager;
import io.trino.server.ResourceGroupInfo;
import io.trino.server.metric.MetricRegistry;
import io.trino.server.metric.MetricSet;
import io.trino.server.metric.MetricSource;
import io.trino.spi.resourcegroups.ResourceGroupState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

public class ResourceGroupMetricSource
        extends MetricSource
{
    private final InternalResourceGroupManager resourceGroupManager;

    @Inject
    public ResourceGroupMetricSource(MetricRegistry metricRegistry,
                                     InternalResourceGroupManager resourceGroupManager)
    {
        super(metricRegistry);
        this.resourceGroupManager = resourceGroupManager;
    }

    @Override
    public List<MetricSet> collect()
    {
        List<MetricSet> metricSetList = new ArrayList<>();

        Set<ResourceGroupInfo> resourceGroupInfoSet = resourceGroupManager.getResourceGroupInfoSet();
        for (ResourceGroupInfo resourceGroupInfo : resourceGroupInfoSet) {
            Map<String, Number> indicators = Map.ofEntries(
                    Map.entry("resourceGroup.numRunningQueries.gauge", resourceGroupInfo.getNumRunningQueries()),
                    Map.entry("resourceGroup.numQueuedQueries.gauge", resourceGroupInfo.getNumQueuedQueries()),
                    Map.entry("resourceGroup.cpuUsageMillis.count", resourceGroupInfo.getCpuUsage().toMillis()),
                    Map.entry("resourceGroup.memoryUsageBytes.gauge", resourceGroupInfo.getMemoryUsage().toBytes()));
            if (ResourceGroupState.FULL.equals(resourceGroupInfo.getState())) {
                indicators.put("resourceGroup.full", 1);
            }

            Map<String, String> metadata = Map.of("groupId", resourceGroupInfo.getId().toString());
            metricSetList.add(new MetricSet(metadata, indicators));
        }

        return metricSetList;
    }
}
