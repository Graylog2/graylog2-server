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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.graylog2.Log;
import org.graylog2.database.MongoConnection;

/**
 * Stream.java: Mar 26, 2011 10:39:40 PM
 *
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Stream {

    private ObjectId id = null;
    private String title = null;
    private List<StreamRule> streamRules = null;

    private DBObject mongoObject = null;

    public Stream (DBObject stream) {
        this.id = (ObjectId) stream.get("_id");
        this.title = (String) stream.get("title");
        this.mongoObject = stream;
    }

    public static ArrayList<Stream> fetchAll() throws Exception {
        if (StreamCache.getInstance().valid()) {
            return StreamCache.getInstance().get();
        }
        
        ArrayList<Stream> streams = new ArrayList<Stream>();

        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("streams");
        DBCursor cur = coll.find(new BasicDBObject());

        while (cur.hasNext()) {
            try {
                streams.add(new Stream(cur.next()));
            } catch (Exception e) {
                Log.warn("Can't fetch stream. Skipping. " + e.toString());
            }
        }

        StreamCache.getInstance().set(streams);

        return streams;
    }

    public List<StreamRule> getStreamRules() {
        if (this.streamRules != null) {
            return this.streamRules;
        }

        ArrayList<StreamRule> rules = new ArrayList<StreamRule>();

        BasicDBList rawRules = (BasicDBList) this.mongoObject.get("streamrules");
        if (rawRules != null && rawRules.size() > 0) {
            for (Object ruleObj : rawRules) {
                StreamRule rule = new StreamRule((DBObject) ruleObj);
                rules.add(rule);
            }
        }

        this.streamRules = rules;
        return rules;
    }

    /**
     * @return the id
     */
    public ObjectId getId() {
        return id;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        this.getStreamRules();
        return this.id.toString() + ":" + this.title;
    }

}