/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.streams;

import com.mongodb.BasicDBObject;
import java.util.HashSet;
import java.util.Map;
import org.bson.types.ObjectId;
import org.elasticsearch.common.collect.Maps;
import org.graylog2.plugin.outputs.MessageOutput;

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

        if (!outputs.containsKey(typeclass)) {
            outputs.put(typeclass, new HashSet<Map<String, String>>());
        }
        
        outputs.get(typeclass).add(outputConfig);
    }
    
    public static BasicDBObject buildInitial(String name) {
        BasicDBObject dbo = new BasicDBObject();
        dbo.put("_id", new ObjectId());
        dbo.put("title", name);
        
        return dbo;
    }
    
}
