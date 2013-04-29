/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.indexer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.admin.indices.stats.IndexShardStats;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.action.admin.indices.stats.ShardStats;
import org.graylog2.Core;
import org.graylog2.plugin.Tools;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class DeflectorInformation {
    
    private Core graylogServer;
    
    private Map<String, IndexStats> indices = Maps.newHashMap();
    private String deflectorTarget;
    private int maxMessagesPerIndex;
    private String serverId;
    
    public DeflectorInformation(Core server) {
        this.graylogServer = server;
    }
    
    public void addIndex(IndexStats index) {
        indices.put(index.getIndex(), index);
    }
    
    public void addIndices(Map<String, IndexStats> indices) {
        this.indices.putAll(indices);
    }

    public void setDeflectorTarget(String target) {
        this.deflectorTarget = target;
    }
    
    public void setConfiguredMaximumMessagesPerIndex(int max) {
        this.maxMessagesPerIndex = max;
    }
    
    public void setCallingNode(String serverId) {
        this.serverId = serverId;
    }
    
    public Map<String, Object> getAsDatabaseObject() {
        Map<String, Object> result = Maps.newHashMap();

        Map<String, Map<String, Object>> indexInformation = Maps.newHashMap();
        for (Map.Entry<String, IndexStats> e : indices.entrySet()) {
            indexInformation.put(e.getKey(), getIndexInformation(e.getValue()));
        }
        
        result.put("server_id", serverId);
        result.put("deflector_target", deflectorTarget);
        result.put("max_messages_per_index", maxMessagesPerIndex);
        result.put("indices", indexInformation);
        result.put("timestamp", Tools.getUTCTimestamp());
        
        return result;
    }
    
    private Map<String, Object> getIndexInformation(IndexStats stats) {
        Map<String, Object> info = Maps.newHashMap();
        info.put("docs", stats.getPrimaries().getDocs().getCount());
        info.put("size", stats.getPrimaries().getStore().getSize().getKb());
        info.put("time_index", stats.getPrimaries().getIndexing().getTotal().getIndexTime().getSeconds());
        info.put("time_query", stats.getPrimaries().getSearch().getTotal().getQueryTime().getSeconds());
        info.put("time_fetch", stats.getPrimaries().getSearch().getTotal().getFetchTime().getSeconds());
        info.put("time_get", stats.getPrimaries().getGet().getTime().getSeconds());
        info.put("shards", getShardInformation(stats));
        
        return info;
    }
    
    private List<Map<String, Object>> getShardInformation(IndexStats stats) {
        List<Map<String, Object>> shards = Lists.newArrayList();
        for(Map.Entry<Integer, IndexShardStats> s : stats.getIndexShards().entrySet()) {
            Iterator<ShardStats> iter = s.getValue().iterator();
            while (iter.hasNext()) {
                ShardStats ss = iter.next();

                Map<String, Object> shard = Maps.newHashMap();

                shard.put("node_hostname", graylogServer.getIndexer().nodeIdToHostName(ss.getShardRouting().currentNodeId()));
                shard.put("node_name", graylogServer.getIndexer().nodeIdToName(ss.getShardRouting().currentNodeId()));
                shard.put("id", ss.getShardId());
                shard.put("node_id", ss.getShardRouting().currentNodeId());
                shard.put("primary", ss.getShardRouting().primary());
                shard.put("is_initializing", ss.getShardRouting().initializing());
                shard.put("is_started", ss.getShardRouting().started());
                shard.put("is_unassigned", ss.getShardRouting().unassigned());
                shard.put("is_relocating", ss.getShardRouting().relocating());
                shard.put("relocating_to", graylogServer.getIndexer().nodeIdToName(ss.getShardRouting().relocatingNodeId()));

                shards.add(shard);
            }
        }
        
        return shards;
    }
    
}
