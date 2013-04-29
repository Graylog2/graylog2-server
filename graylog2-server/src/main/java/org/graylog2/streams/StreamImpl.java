/**
 * Copyright 2011, 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.streams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.Persistable;
import org.graylog2.database.Persisted;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StreamImpl extends Persisted implements Stream, Persistable {

    private static final String COLLECTION = "streams";
    
    public StreamImpl(Map<String, Object> fields, Core core) {
    	super(COLLECTION, core, fields);
    }

    protected StreamImpl(ObjectId id, Map<String, Object> fields, Core core) {
    	super(COLLECTION, core, id, fields);
    }
    
    @SuppressWarnings("unchecked")
	public static StreamImpl load(ObjectId id, Core core) throws NotFoundException {
    	BasicDBObject o = (BasicDBObject) get(id, core, COLLECTION);

    	if (o == null) {
    		throw new NotFoundException();
    	}
    	
    	return new StreamImpl((ObjectId) o.get("_id"), o.toMap(), core);
    }
    
    public static List<Stream> loadAllEnabled(Core core) {
        return loadAllEnabled(core, new HashMap<String, Object>());
    }
    
    @SuppressWarnings("unchecked")
	public static List<Stream> loadAllEnabled(Core core, Map<String, Object> additionalQueryOpts) {
    	List<Stream> streams = Lists.newArrayList();
    	
    	DBObject query = new BasicDBObject();
        query.put("disabled", new BasicDBObject("$ne", true));
    	
        // putAll() is not working with BasicDBObject.
        for (Map.Entry<String, Object> o : additionalQueryOpts.entrySet()) {
        	query.put(o.getKey(), o.getValue());
        }
        
        List<DBObject> results = query(query, core, COLLECTION);
        for (DBObject o : results) {
        	streams.add(new StreamImpl((ObjectId) o.get("_id"), o.toMap(), core));
        }

    	return streams;
    }
    
    
    
    
    
    
    
    public Set<Map<String, String>> getOutputConfigurations(String className) {
    	return null;
    }
    
    public boolean hasConfiguredOutputs(String typeClass) {
    	return false;
    }
    
    public boolean inAlarmGracePeriod() {
    	return true;
    }
    
    public void setLastAlarm(int timestamp, Core server) {
    }
    
    public Set<String> getAlarmCallbacks() {
    	return Sets.newHashSet();
    }

    public static Map<String, String> nameMap(Core server) {
    	return Maps.newHashMap();
    }
    
	@Override
	public List<StreamRule> getStreamRules() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAlarmTimespan() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAlarmMessageLimit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAlarmPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<AlarmReceiver> getAlarmReceivers(GraylogServer server) {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public String toString() {
        this.getStreamRules();
        return this.id.toString() + ":" + this.getTitle();
    }

	@Override
	public ObjectId getId() {
		return this.id;
	}

	@Override
	public String getTitle() {
		return (String) fields.get("title");
	}
	
	public Map<String, Object> asMap() {
		// We work on the result a bit to allow correct JSON serializing.
		Map<String, Object> result = Maps.newHashMap(fields);
		result.remove("_id");
		result.put("id", ((ObjectId) fields.get("_id")).toStringMongod());
		
		if (!result.containsKey("rules")) {
			result.put("rules", Maps.newHashMap());
		}
		
		return result;
	}

}