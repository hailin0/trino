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

import io.trino.memory.ClusterMemoryManager;
import io.trino.memory.MemoryInfo;
import io.trino.server.metric.MetricRegistry;
import io.trino.server.metric.MetricSet;
import io.trino.server.metric.MetricSource;
import io.trino.spi.memory.MemoryPoolId;
import io.trino.spi.memory.MemoryPoolInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

public class NodeMemoryMetricSource
        extends MetricSource
{
    private ClusterMemoryManager clusterMemoryManager;

    @Inject
    public NodeMemoryMetricSource(MetricRegistry metricRegistry,
                                  ClusterMemoryManager clusterMemoryManager)
    {
        super(metricRegistry);
        this.clusterMemoryManager = clusterMemoryManager;
    }

    @Override
    public List<MetricSet> collect()
    {
        List<MetricSet> metricSetList = new ArrayList<>();

        Map<String, Optional<MemoryInfo>> workerMemoryInfos = clusterMemoryManager.getWorkerMemoryInfo();
        for (Map.Entry<String, Optional<MemoryInfo>> memoryInfoEntry : workerMemoryInfos.entrySet()) {
            Optional<MemoryInfo> optional = memoryInfoEntry.getValue();
            if (optional.isEmpty()) {
                continue;
            }

            String workerId = memoryInfoEntry.getKey();
            MemoryInfo memoryInfo = optional.get();
            Map<MemoryPoolId, MemoryPoolInfo> memoryInfoPools = memoryInfo.getPools();
            for (Map.Entry<MemoryPoolId, MemoryPoolInfo> memoryPoolInfoEntry : memoryInfoPools.entrySet()) {
                MemoryPoolId memoryPoolId = memoryPoolInfoEntry.getKey();
                MemoryPoolInfo memoryPoolInfo = memoryPoolInfoEntry.getValue();

                Map<String, String> metadata = Map.of("workerId", workerId, "memoryPoolId", memoryPoolId.getId());
                Map<String, Number> indicators = Map.of(
                        "worker.memoryPool.maxBytes.gauge", memoryPoolInfo.getMaxBytes(),
                        "worker.memoryPool.freeBytes.gauge", memoryPoolInfo.getFreeBytes(),
                        "worker.memoryPool.reservedBytes.gauge", memoryPoolInfo.getReservedBytes(),
                        "worker.memoryPool.reservedRevocableBytes.gauge", memoryPoolInfo.getReservedRevocableBytes());
                MetricSet metricSet = new MetricSet(metadata, indicators);
                metricSetList.add(metricSet);
            }
        }
        return metricSetList;
    }
}
