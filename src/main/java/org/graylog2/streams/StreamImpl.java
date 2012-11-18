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

import org.graylog2.plugin.streams.Stream;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.graylog2.Core;
import org.graylog2.Tools;
import org.graylog2.alarms.AlarmReceiverImpl;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.alarms.AlarmReceiver;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.users.User;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamImpl implements Stream {

    private static final Logger LOG = Logger.getLogger(StreamImpl.class);

    private final ObjectId id;
    private final String title;
    private final int alarmTimespan;
    private final int alarmMessageLimit;
    private final boolean alarmForce;
    private final int alarmPeriod;
    private final int lastAlarm;
    
    private final Map<String, Set<Map<String, String>>> outputs;

    private List<StreamRule> streamRules;
    private Set<String> alarmCallbacks;

    private final DBObject mongoObject;

    public StreamImpl (DBObject stream) {
        this.id = (ObjectId) stream.get("_id");
        this.title = (String) stream.get("title");
        
        if (stream.get("alarm_timespan") != null) {
            this.alarmTimespan = (Integer) stream.get("alarm_timespan");
        } else {
            this.alarmTimespan = -1;
        }
        
        if (stream.get("alarm_limit") != null) {
            this.alarmMessageLimit = (Integer) stream.get("alarm_limit");
        } else {
            this.alarmMessageLimit = -1;
        }
        
        if (stream.get("alarm_force") != null) {
            this.alarmForce = (Boolean) stream.get("alarm_force");
        } else {
            this.alarmForce = false;
        }
        
        if (stream.get("alarm_period") != null) {
            this.alarmPeriod = (Integer) stream.get("alarm_period");
        } else {
            this.alarmPeriod = 0;
        }
        
        if (stream.get("last_alarm") != null) {
            this.lastAlarm = (Integer) stream.get("last_alarm");
        } else {
            this.lastAlarm = 0;
        }
        
        if (stream.get("outputs") != null) {
            this.outputs = buildOutputsFromMongoDoc(stream);
        } else {
            this.outputs = Maps.newHashMap();
        }
        
        this.mongoObject = stream;
    }
    
    public static Set<Stream> fetchAllEnabled(Core server) {
        Map<String, Object> emptyMap = Maps.newHashMap();
        return fetchAllEnabled(server, emptyMap);
    }

    public static Set<Stream> fetchAllEnabled(Core server, Map<String, Object> additionalQueryOpts) {
        StreamCache streamCache = StreamCache.getInstance();
        if (streamCache.valid()) {
            return streamCache.get();
        }

        Set<Stream> streams = Sets.newHashSet();

        DBCollection coll = server.getMongoConnection().getDatabase().getCollection("streams");
        DBObject query = new BasicDBObject();
        query.put("disabled", new BasicDBObject("$ne", true));
        
        // query.putAll() is not working
        for (Map.Entry<String, Object> o : additionalQueryOpts.entrySet()) {
             query.put(o.getKey(), o.getValue());
        }
            
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
        
        List objs = (BasicDBList) this.mongoObject.get("alarm_callbacks");
        if (objs != null) {
            for (Object obj : objs) {
                DBObject callback = (BasicDBObject) obj;
                String typeclass = (String) callback.get("typeclass");
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
        return outputs.get(className);
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
        int graceLine = lastAlarm+(alarmPeriod*60);
        LOG.debug("Last alarm of stream <" + getId() + "> was at [" + lastAlarm + "]. "
                + "Grace period ends at [" + graceLine + "]. It now is [" + now + "].");
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

    private Map<String, Set<Map<String, String>>> buildOutputsFromMongoDoc(DBObject stream) {
        Map<String, Set<Map<String, String>>> o = Maps.newHashMap();
        
        List objs = (BasicDBList) stream.get("outputs");
        
        if (objs == null || objs.isEmpty()) {
            return o;
        }
        
        for (Object obj : objs) {
            try {
                DBObject output = (BasicDBObject) obj;
                String typeclass = (String) output.get("typeclass");

                if (!o.containsKey(typeclass)) {
                    o.put(typeclass, new HashSet<Map<String, String>>());
                }
                
                // ZOMG we need an ODM in the next version.
                Map<String, String> outputConfig = Maps.newHashMap();
                for (Object key : output.toMap().keySet()) {
                    outputConfig.put(key.toString(), output.get(key.toString()).toString());
                }
                
                o.get(typeclass).add(outputConfig);
            } catch(Exception e) {
                LOG.warn("Could not read stream output.", e);
                continue;
            }
        }
        
        return o;
    }
    
}