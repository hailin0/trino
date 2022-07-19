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

import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.airlift.log.Logger;

import io.trino.plugin.base.cache.EvictableCache;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

public class MetricCollector
{
    private static final Logger LOG = Logger.get(MetricCollector.class);

    private static final long COLLECT_INTERVAL_SECONDS = 1;
    private static final long CACHE_EXPIRE_SECONDS = 60;

    private ScheduledExecutorService executor;
    private final MetricRegistry metricRegistry;
    private EvictableCache<Long, List<MetricSet>> metricCache;

    private long lastPullMetricTs = -1;
    private List<MetricSet> lastMetricSetList = Collections.emptyList();

    @Inject
    public MetricCollector(MetricRegistry metricRegistry)
    {
        this.metricRegistry = metricRegistry;
    }

    @PostConstruct
    public void start()
    {
        metricCache = EvictableCache.buildWith(
                CacheBuilder.newBuilder().expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS));

        ThreadFactory factory =
                new ThreadFactoryBuilder()
                        .setNameFormat("metric-collector-%s")
                        .setUncaughtExceptionHandler((t, e) -> {
                            LOG.error(e, "Uncaught exception thrown by metric collector thread: %d", t.getId());
                        })
                        .build();
        executor = Executors.newSingleThreadScheduledExecutor(factory);
        executor.scheduleAtFixedRate(this::collect, 0, COLLECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop()
    {
        if (null != metricCache) {
            metricCache.invalidateAll();
        }
        if (null != executor) {
            executor.shutdownNow();
        }
    }

    private void collect()
    {
        long currentTs = System.currentTimeMillis() / 1000 * 1000;
        try {
            List<MetricSet> metricSetList = metricRegistry.collect();
            metricCache.get(currentTs, () -> computeNewMetricSet(lastMetricSetList, metricSetList, currentTs));

            lastMetricSetList = metricSetList;
        }
        catch (Exception e) {
            LOG.error(e, "Failed to collect metrics at %d", currentTs);
        }
    }

    public List<MetricSet> pull()
    {
        List<MetricSet> metricSetList = getMetricSetAfter(lastPullMetricTs);
        if (!metricSetList.isEmpty()) {
            lastPullMetricTs = metricSetList.get(metricSetList.size() - 1).getTimestamp();
        }
        return metricSetList;
    }

    public List<MetricSet> inspect()
    {
        return getMetricSetAfter(-1);
    }

    private List<MetricSet> getMetricSetAfter(long timestamp)
    {
        List<MetricSet> metricSetList = new ArrayList<>();
        for (Long metricTs : metricCache.asMap().keySet()) {
            if (metricTs > timestamp) {
                List<MetricSet> cacheItem = metricCache.getIfPresent(metricTs);
                if (cacheItem != null) {
                    metricSetList.addAll(cacheItem);
                }
            }
        }
        Collections.sort(metricSetList, Comparator.comparingLong(MetricSet::getTimestamp));
        return metricSetList;
    }

    private static List<MetricSet> computeNewMetricSet(List<MetricSet> lastMetricSetList,
                                                       List<MetricSet> currentMetricSetList,
                                                       long timestamp)
    {
        Map<MetricKey, Number> lastMetricMap = new HashMap<>();

        for (MetricSet lastMetricSet : lastMetricSetList) {
            Map<String, String> metadata = lastMetricSet.getMetadata();
            for (Map.Entry<String, Number> indicator : lastMetricSet.getIndicators().entrySet()) {
                String metricName = indicator.getKey();
                Number metricValue = indicator.getValue();
                lastMetricMap.put(new MetricKey(metricName, metadata), metricValue);
            }
        }

        List<MetricSet> newMetricSetList = new ArrayList<>();

        for (MetricSet currentMetricSet : currentMetricSetList) {
            Map<String, String> metadata = currentMetricSet.getMetadata();
            Map<String, Number> newIndicators = new HashMap<>();
            for (Map.Entry<String, Number> indicator : currentMetricSet.getIndicators().entrySet()) {
                String metricName = indicator.getKey();
                Number metricValue = indicator.getValue();
                MetricKey metricKey = new MetricKey(metricName, metadata);
                if (MetricSet.isCountMetric(metricName)) {
                    Number lastMetricValue = lastMetricMap.get(metricKey);
                    if (lastMetricValue == null) {
                        newIndicators.put(metricName, metricValue);
                    }
                    else {
                        long newMetricValue = metricValue.longValue() - lastMetricValue.longValue();
                        newIndicators.put(metricName, newMetricValue < 0 ? 0 : newMetricValue);
                    }
                }
                else {
                    newIndicators.put(metricName, metricValue);
                }
            }
            newMetricSetList.add(new MetricSet(metadata, newIndicators, timestamp));
        }

        return newMetricSetList;
    }

    private static class MetricKey
    {
        String metricName;
        Map<String, String> metadata;

        public MetricKey(String metricName, Map<String, String> metadata)
        {
            this.metricName = metricName;
            this.metadata = metadata;
        }
    }
}
