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
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.elasticsearch.indices.InvalidAliasNameException;
import org.graylog2.Core;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.Tools;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * Format of actual indexes behind the Deflector:
 *   [configured_prefix]_1
 *   [configured_prefix]_2
 *   [configured_prefix]_3
 *   ...
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Deflector { // extends Ablenkblech
    
    private static final Logger LOG = LoggerFactory.getLogger(Deflector.class);
    
    public static final String DEFLECTOR_NAME = "graylog2_deflector";
    
    Core server;
    
    public Deflector(Core server) {
        this.server = server;
    }
    
    public boolean isUp() {
        return this.server.getIndexer().indices().aliasExists(DEFLECTOR_NAME);
    }
    
    public void setUp() {
        // Check if there already is an deflector index pointing somewhere.
        if (isUp()) {
            LOG.info("Found deflector alias <{}>. Using it.", DEFLECTOR_NAME);
        } else {
            LOG.info("Did not find an deflector alias. Setting one up now.");

            try {
            // Do we have a target index to point to?
            try {
                String currentTarget = getNewestTargetName();
                LOG.info("Pointing to already existing index target <{}>", currentTarget);
                
                pointTo(currentTarget);
            } catch(NoTargetIndexException ex) {
                final String msg = "There is no index target to point to. Creating one now.";
                LOG.info(msg);
                server.getActivityWriter().write(new Activity(msg, Deflector.class));
                
                cycle(); // No index, so automatically cycling to a new one.
            }
            } catch (InvalidAliasNameException e) {
                LOG.error("Seems like there already is an index called [{}]", DEFLECTOR_NAME);
            }
        }
    }
    
    public void cycle() {
        LOG.info("Cycling deflector to next index now.");
        int oldTargetNumber;
        
        try {
            oldTargetNumber = getNewestTargetNumber();
        } catch (NoTargetIndexException ex) {
            oldTargetNumber = -1;
        }
        
        int newTargetNumber = oldTargetNumber+1;
        
        String newTarget = buildIndexName(server.getConfiguration().getElasticSearchIndexPrefix(), newTargetNumber);
        String oldTarget = buildIndexName(server.getConfiguration().getElasticSearchIndexPrefix(), oldTargetNumber);
        
        if (oldTargetNumber == -1) {
            LOG.info("Cycling from <none> to <{}>", newTarget);
        } else {
            LOG.info("Cycling from <{}> to <{}>", oldTarget, newTarget);
        }
        
        // Create new index.
        LOG.info("Creating index target <{}>...", newTarget);
        server.getIndexer().indices().create(newTarget);
        updateIndexRanges(newTarget);

        LOG.info("Done!");
        
        // Point deflector to new index.
        LOG.info("Pointing deflector to new target index....");

        Activity activity = new Activity(Deflector.class);
        if (oldTargetNumber == -1) {
            // Only pointing, not cycling.
            pointTo(newTarget);
            activity.setMessage("Cycled deflector from <none> to <" + newTarget + ">");
        } else {
            // Re-pointing from existing old index to the new one.
            pointTo(newTarget, oldTarget);
            LOG.info("Flushing old index <{}>.", oldTarget);
            server.getIndexer().indices().flush(oldTarget);

            LOG.info("Setting old index <{}> to read-only.", oldTarget);
            server.getIndexer().indices().setReadOnly(oldTarget);
            activity.setMessage("Cycled deflector from <" + oldTarget + "> to <" + newTarget + ">");

            try {
                server.getSystemJobManager().submit(new OptimizeIndexJob(server, oldTarget));
            } catch (SystemJobConcurrencyException e) {
                // The concurrency limit is very high. This should never happen.
                LOG.error("Cannot optimize index <{}>.", oldTarget, e);
            }
        }

        LOG.info("Done!");

        server.getActivityWriter().write(activity);
    }
    
    public int getNewestTargetNumber() throws NoTargetIndexException {
        Map<String, IndexStats> indexes = this.server.getIndexer().indices().getAll();
        if (indexes.isEmpty()) {
            throw new NoTargetIndexException();
        }
 
        List<Integer> indexNumbers = new ArrayList<Integer>();
        
        for(Map.Entry<String, IndexStats> e : indexes.entrySet()) {
            if (!ourIndex(e.getKey())) {
                continue;
            }
            
            try {
                indexNumbers.add(extractIndexNumber(e.getKey()));
            } catch (NumberFormatException ex) {
                continue;
            }
        }

        if (indexNumbers.isEmpty()) {
            throw new NoTargetIndexException();
        }
        
        return Collections.max(indexNumbers);
    }
    
    public String[] getAllDeflectorIndexNames() {
        List<String> indices = Lists.newArrayList();
        
        for(Map.Entry<String, IndexStats> e : server.getIndexer().indices().getAll().entrySet()) {
            String name = e.getKey();
            
            if (ourIndex(name)) {
                indices.add(name);
            }
        }
        
        return indices.toArray(new String[0]);
    }
    
    public Map<String, IndexStats> getAllDeflectorIndices() {
        Map<String, IndexStats> result = Maps.newHashMap();
        for(Map.Entry<String, IndexStats> e : server.getIndexer().indices().getAll().entrySet()) {
            String name = e.getKey();
            
            if (ourIndex(name)) {
                result.put(name, e.getValue());
            }
        }
        
        return result;
    }
    
    public String getNewestTargetName() throws NoTargetIndexException {
        return buildIndexName(this.server.getConfiguration().getElasticSearchIndexPrefix(), getNewestTargetNumber());
    }
    
    public static String buildIndexName(String prefix, int number) {
        return prefix + "_" + number;
    }
    
    public static int extractIndexNumber(String indexName) throws NumberFormatException {
        String[] parts = indexName.split("_");
        
        try {
            return Integer.parseInt(parts[parts.length-1]);
        } catch(Exception e) {
            LOG.debug("Could not extract index number from index <{}>.", indexName);
            throw new NumberFormatException();
        }
    }
    
    private boolean ourIndex(String indexName) {
        return indexName != DEFLECTOR_NAME && indexName.startsWith(server.getConfiguration().getElasticSearchIndexPrefix() + "_");
    }
    
    public void pointTo(String newIndex, String oldIndex) {
        server.getIndexer().cycleAlias(DEFLECTOR_NAME, newIndex, oldIndex);
    }
    
    public void pointTo(String newIndex) {
        server.getIndexer().cycleAlias(DEFLECTOR_NAME, newIndex);
    }

    private void updateIndexRanges(String index) {
        Map<String, Object> params = Maps.newHashMap();
        params.put("index", index);
        params.put("start", Tools.getUTCTimestamp());

        IndexRange range = new IndexRange(server, params);
        range.saveWithoutValidation();
    }

    public String getCurrentActualTargetIndex() {
        return server.getIndexer().indices().aliasTarget(Deflector.DEFLECTOR_NAME);
    }
}