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

import io.airlift.node.NodeInfo;
import io.trino.memory.ClusterMemoryManager;
import io.trino.server.metric.MetricRegistry;
import io.trino.server.metric.MetricSet;
import io.trino.server.metric.MetricSource;
import io.trino.server.ui.ClusterStatsResource;
import io.trino.spi.memory.MemoryPoolId;
import io.trino.spi.memory.MemoryPoolInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class ClusterResourceMetricSource
        extends MetricSource
{
    private final NodeInfo nodeInfo;
    private final ClusterMemoryManager clusterMemoryManager;
    private final ClusterStatsResource clusterStatsResource;

    @Inject
    public ClusterResourceMetricSource(MetricRegistry metricRegistry,
                                       NodeInfo nodeInfo,
                                       ClusterMemoryManager clusterMemoryManager,
                                       ClusterStatsResource clusterStatsResource)
    {
        super(metricRegistry);
        this.nodeInfo = nodeInfo;
        this.clusterMemoryManager = clusterMemoryManager;
        this.clusterStatsResource = clusterStatsResource;
    }

    @Override
    public List<MetricSet> collect()
    {
        List<MetricSet> metricSetList = new ArrayList<>();
        metricSetList.add(collectClusterStats());
        metricSetList.addAll(collectMemoryPoolInfo());
        return metricSetList;
    }

    private MetricSet collectClusterStats()
    {
        ClusterStatsResource.ClusterStats clusterStats = clusterStatsResource.getClusterStats();
        Map<String, Number> indicators = Map.ofEntries(
                Map.entry("cluster.runningQueries.gauge", clusterStats.getRunningQueries()),
                Map.entry("cluster.blockedQueries.gauge", clusterStats.getBlockedQueries()),
                Map.entry("cluster.queuedQueries.gauge", clusterStats.getQueuedQueries()),
                Map.entry("cluster.activeCoordinators.gauge", clusterStats.getActiveCoordinators()),
                Map.entry("cluster.activeWorkers.gauge", clusterStats.getActiveWorkers()),
                Map.entry("cluster.runningDrivers.gauge", clusterStats.getRunningDrivers()),
                Map.entry("cluster.totalAvailableProcessors.gauge", clusterStats.getTotalAvailableProcessors()),
                Map.entry("cluster.reservedMemory.gauge", clusterStats.getReservedMemory()),
                Map.entry("cluster.totalInputRows.count", clusterStats.getTotalInputRows()),
                Map.entry("cluster.totalInputBytes.count", clusterStats.getTotalInputBytes()),
                Map.entry("cluster.totalCpuTimeSecs.count", clusterStats.getTotalCpuTimeSecs()),
                Map.entry("cluster.numberOfLeakedQueries.count", clusterMemoryManager.getNumberOfLeakedQueries()),
                Map.entry("cluster.queriesKilledDueToOutOfMemory.count", clusterMemoryManager.getQueriesKilledDueToOutOfMemory()));

        Map<String, String> metadata = Map.of("environment", nodeInfo.getEnvironment());
        return new MetricSet(metadata, indicators);
    }

    private List<MetricSet> collectMemoryPoolInfo()
    {
        List<MetricSet> metricSetList = new ArrayList<>();

        Map<MemoryPoolId, MemoryPoolInfo> memoryPoolInfos = clusterMemoryManager.getMemoryPoolInfo();
        for (Map.Entry<MemoryPoolId, MemoryPoolInfo> memoryPoolInfoEntry : memoryPoolInfos.entrySet()) {
            MemoryPoolId memoryPoolId = memoryPoolInfoEntry.getKey();
            MemoryPoolInfo memoryPoolInfo = memoryPoolInfoEntry.getValue();

            Map<String, String> metadata = Map.of(
                    "environment", nodeInfo.getEnvironment(),
                    "memoryPoolId", memoryPoolId.getId());
            Map<String, Number> indicators = Map.of(
                    "cluster.memoryPool.maxBytes.gauge", memoryPoolInfo.getMaxBytes(),
                    "cluster.memoryPool.freeBytes.gauge", memoryPoolInfo.getFreeBytes(),
                    "cluster.memoryPool.reservedBytes.gauge", memoryPoolInfo.getReservedBytes(),
                    "cluster.memoryPool.reservedRevocableBytes.gauge", memoryPoolInfo.getReservedRevocableBytes());
            MetricSet metricSet = new MetricSet(metadata, indicators);
            metricSetList.add(metricSet);
        }
        return metricSetList;
    }
}
