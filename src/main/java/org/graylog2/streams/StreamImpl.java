/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.plugin.Tools;
import org.graylog2.alarms.AlarmReceiverImpl;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamImpl implements Stream {

    private static final Logger LOG = LoggerFactory.getLogger(StreamImpl.class);

    private final ObjectId id;
    private final String title;
    private final int alarmTimespan;
    private final int alarmMessageLimit;
    private final boolean alarmActive;
    private final boolean alarmForce;
    private final int alarmPeriod;
    private final int lastAlarm;
    private final DBObject mongoObject;
    
    private transient List<StreamRule> streamRules;
    private transient Set<String> alarmCallbacks;
    protected transient Map<String, Set<Map<String, String>>> outputs;

    public StreamImpl (DBObject stream) {
        this.id = (ObjectId) stream.get("_id");
        this.title = (String) stream.get("title");
        this.alarmTimespan = getDefaultedValue(stream, "alarm_timespan", -1);
        this.alarmMessageLimit = getDefaultedValue(stream, "alarm_limit", -1);
        this.alarmActive = getDefaultedValue(stream, "alarm_active", false);
        this.alarmForce = getDefaultedValue(stream, "alarm_force", false);
        this.alarmPeriod = getDefaultedValue(stream, "alarm_period", 0);
        this.lastAlarm = getDefaultedValue(stream, "last_alarm", 0);
        this.mongoObject = stream;
    }
    
    @SuppressWarnings("unchecked")
	private static <T> T getDefaultedValue(DBObject object, String field, T defaultValue) {
    	Object value = object.get(field);
    	return (null == value) ? defaultValue : (T) value;
    }
    
    public static Set<StreamImpl> fetchAllEnabled(Core server) {
        StreamCache streamCache = StreamCache.getInstance();
        if (streamCache.valid()) {
            return streamCache.get();
        }

        Set<StreamImpl> streams = Sets.newHashSet();

        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("streams");
        DBObject query = new BasicDBObject();
        query.put("disabled", new BasicDBObject("$ne", true));
        
        DBCursor cur = coll.find(query);

        while (cur.hasNext()) {
            try {
                streams.add(new StreamImpl(cur.next()));
            } catch (Exception e) {
                LOG.warn("Can't fetch stream. Skipping. " + e.getMessage(), e);
            }
        }

        streamCache.set(streams);

        return streams;
    }
    
    public static Map<String, String> nameMap(Core server) {
        Map<String, String> streams = Maps.newHashMap();
        
        for(Stream stream : fetchAllEnabled(server)) {
            streams.put(stream.getId().toString(), stream.getTitle());
        }
        
        return streams;
    }

    @Override
    public List<StreamRule> getStreamRules() {
        if (this.streamRules != null) {
            return this.streamRules;
        }

        List<StreamRule> rules = Lists.newArrayList();

        BasicDBList rawRules = (BasicDBList) this.mongoObject.get("streamrules");
        if (rawRules != null && rawRules.size() > 0) {
            for (Object ruleObj : rawRules) {
                try {
                    StreamRule rule = new StreamRuleImpl((DBObject) ruleObj);
                    rules.add(rule);
                } catch (Exception e) {
                    LOG.warn("Skipping stream rule in Stream.getStreamRules(): " + e.getMessage(), e);
                }
            }
        }

        this.streamRules = rules;
        return rules;
    }
    
    public Set<String> getAlarmCallbacks() {
        if (this.alarmCallbacks != null) {
            return this.alarmCallbacks;
        }
        
        Set<String> callbacks = Sets.newTreeSet();
        List<Object> objs = (BasicDBList) this.mongoObject.get("alarm_callbacks");
        
        if (objs != null) {
            for (Object obj : objs) {
                String typeclass = (String) obj;
                if (typeclass != null && !typeclass.isEmpty()) {
                    callbacks.add(typeclass);
                }
            }
        }
        
        this.alarmCallbacks = callbacks;
        return callbacks;
    }

    @Override
    public Set<AlarmReceiver> getAlarmReceivers(GraylogServer server) {
        Core core = (Core) server;

        Set<User> users;
        
        if (alarmForce) {
            // Alarm notification is forced for all users. Fetch them all.
            users = User.fetchAll(core);
        } else {
            // Fetch only users that have subscribed to alarms of this stream.
            Map<String, Object> conditions = Maps.newHashMap();
            Map<String, Set<ObjectId>> userCondition = Maps.newHashMap();
            userCondition.put("$in", getAlarmedUserIds(core));
            conditions.put("_id", userCondition);
            users = User.fetchAll(core, conditions);
        }
        
        return usersToAlarmReceivers(users);
    }
    
    public Set<Map<String, String>> getOutputConfigurations(String className) {
        if (this.outputs == null) {
        	this.outputs = buildOutputsFromMongoList((BasicDBList) this.mongoObject.get("outputs"));
        }

        return outputs.get(className);
    }
    
    public boolean hasConfiguredOutputs(String typeClass) {
        Set<Map<String, String>> oc = getOutputConfigurations(typeClass);
        return oc != null && !oc.isEmpty();
    }
    
    @Override
    public ObjectId getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public int getAlarmTimespan() {
        return alarmTimespan;
    }

    @Override
    public int getAlarmMessageLimit() {
        return alarmMessageLimit;
    }

    @Override
    public int getAlarmPeriod() {
        return alarmPeriod;
    }
    
    public boolean isAlarmActive()
	{
		return alarmActive;
	}
    
    public void setLastAlarm(int timestamp, Core server) {
        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("streams");
        DBObject query = new BasicDBObject();
        query.put("_id", this.id);
        
        DBObject stream = coll.findOne(query);
        stream.put("last_alarm", timestamp);

        coll.update(query, stream);
    }
    
    public boolean inAlarmGracePeriod() {
        int now = Tools.getUTCTimestamp();
        int graceLine = lastAlarm+(alarmPeriod*60)-1;
        LOG.debug("Last alarm of stream <{}> was at [{}]. Grace period ends at [{}]. It now is [{}].",
                new Object[] { getId(), lastAlarm, graceLine, now });
        return now <= graceLine;
    }
    
    @Override
    public String toString() {
        this.getStreamRules();
        return this.id.toString() + ":" + this.title;
    }
    
    private Set<ObjectId> getAlarmedUserIds(Core server) {
        Set<ObjectId> userIds = Sets.newHashSet();
        
        // ZOMG this alerted_streams stuff suck so hard, but we keep it for backwards-compat.
        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("alerted_streams");
        DBObject query = new BasicDBObject();
        query.put("stream_id", this.id);
        
        DBCursor cur = coll.find(query);

        while (cur.hasNext()) {
            DBObject x = cur.next(); // I don't even know how I should call this lol
            userIds.add((ObjectId) x.get("user_id"));
        }
        
        return userIds;
    }

    private Set<AlarmReceiver> usersToAlarmReceivers(Set<User> users) {
        Set <AlarmReceiver> receivers = Sets.newHashSet();
        
        for(User user : users) {
            AlarmReceiverImpl receiver = new AlarmReceiverImpl(user.getId().toString());
            receiver.addAddresses(user.getTransports());

            receivers.add(receiver);
        }
        
        return receivers;
    }
    
    private Map<String, Set<Map<String, String>>> buildOutputsFromMongoList(List<Object> objs)
	{
		if (objs == null || objs.isEmpty()) {
            return Maps.newHashMapWithExpectedSize(0);
        }
        
        Map<String, Set<Map<String, String>>> o = Maps.newHashMap();
        
        for (Object obj : objs) {
            try {
                DBObject output = (BasicDBObject) obj;
                
                // ZOMG we need an ODM in the next version.
                Map<String, String> outputConfig = Maps.newHashMap();
                for (String key : output.keySet()) {
                    String value = output.get(key).toString();
					outputConfig.put(key, value);
                }
                
                String typeclass = (String) output.get("typeclass");
                Set<Map<String, String>> maps = o.get(typeclass);
                if (null == maps) {
					o.put(typeclass, maps = Sets.newHashSet());
                }
                
                maps.add(outputConfig);
            } catch(Exception e) {
                LOG.warn("Could not read stream output.", e);
                continue;
            }
        }
        
        return o;
	}

}