/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.streams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.common.collect.Maps;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.StreamRule;

import com.mongodb.BasicDBObject;

/**
 *
 * @author lennart.koopmann
 */
public class FakeStream extends StreamImpl {
    
    public FakeStream(String name) {
        super(buildInitial(name));
    }
    
    public void addOutput(MessageOutput output) {
        String typeclass = output.getClass().getCanonicalName();
        
        Map<String, String> outputConfig = Maps.newHashMap();
        outputConfig.put("description", "TEST");
        outputConfig.put("id", new ObjectId().toString());
        outputConfig.put("typeclass", typeclass);

        if(null == outputs) {
            outputs = new HashMap<String, Set<Map<String, String>>>();
        }
        
        if (!outputs.containsKey(typeclass)) {
            outputs.put(typeclass, new HashSet<Map<String, String>>());
        }
        
        outputs.get(typeclass).add(outputConfig);
    }
    
    public void addRule(StreamRuleImpl rule) {
        if(streamRules == null) {
            streamRules = new ArrayList<StreamRule>();
        }
        streamRules.add(rule);
    }
    
    public static BasicDBObject buildInitial(String name) {
        BasicDBObject dbo = new BasicDBObject();
        dbo.put("_id", new ObjectId());
        dbo.put("title", name);
        
        return dbo;
    }
    
}
