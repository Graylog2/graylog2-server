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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import org.graylog2.Log;
import org.graylog2.database.MongoConnection;


/**
 * StreamRule.java: Mar 17, 2011 10:27:48 PM
 *
 * [description]
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamRule {
    public final static int ALL_STREAMS = 0;

    private String objectId = null;
    private int streamId = 0;
    private int ruleType = 0;
    private String value = null;

    public StreamRule(String objectId, int streamId, int ruleType, String value) {
        this.objectId = objectId;
        this.streamId = streamId;
        this.ruleType = ruleType;
        this.value = value;
    }

    public static List<StreamRule> fetchAllOfStream(int streamId) {
        ArrayList<StreamRule> streams = new ArrayList<StreamRule>();

        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("streamrules");
        DBObject q = new BasicDBObject();

        if (streamId != ALL_STREAMS) {
            q.put("stream_id", streamId);
        }
        
        DBCursor cur = coll.find(q);

        while (cur.hasNext()) {
            try {
                DBObject doc = cur.next();
                StreamRule rule = new StreamRule(
                        (String) doc.get("_id"),
                        Integer.parseInt((String) doc.get("stream_id")),
                        Integer.parseInt((String) doc.get("rule_type")),
                        (String) doc.get("value")
                );
            } catch (Exception e) {
                Log.warn("Skipping stream rule: " + e.toString());
            }
        }

        return streams;
    }

    /**
     * @return the objectId
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * @param objectId the objectId to set
     */
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    /**
     * @return the streamId
     */
    public int getStreamId() {
        return streamId;
    }

    /**
     * @param streamId the streamId to set
     */
    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    /**
     * @return the ruleType
     */
    public int getRuleType() {
        return ruleType;
    }

    /**
     * @param ruleType the ruleType to set
     */
    public void setRuleType(int ruleType) {
        this.ruleType = ruleType;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }



}
