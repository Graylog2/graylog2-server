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

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.forwarders.ForwardEndpoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream.java: Mar 26, 2011 10:39:40 PM
 *
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Stream {

    private static final Logger LOG = Logger.getLogger(Stream.class);

    private ObjectId id = null;
    private String title = null;

    private List<StreamRule> streamRules = null;
    private List<ForwardEndpoint> forwardedTo = null;

    private DBObject mongoObject = null;

    public Stream (DBObject stream) {
        this.id = (ObjectId) stream.get("_id");
        this.title = (String) stream.get("title");
        this.mongoObject = stream;
    }

    public static ArrayList<Stream> fetchAll() {
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
                LOG.warn("Can't fetch stream. Skipping. " + e.getMessage(), e);
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
                try {
                    StreamRule rule = new StreamRule((DBObject) ruleObj);
                    rules.add(rule);
                } catch (Exception e) {
                    LOG.warn("Skipping stream rule in Stream.getStreamRules(): " + e.getMessage(), e);
                    continue;
                }
            }
        }

        this.streamRules = rules;
        return rules;
    }

    public List<ForwardEndpoint> getForwardedTo() {
        if (this.forwardedTo != null) {
            return this.forwardedTo;
        }
        
        ArrayList<ForwardEndpoint> fwds = new ArrayList<ForwardEndpoint>();

        BasicDBList rawFwds = (BasicDBList) this.mongoObject.get("forwarders");
        if (rawFwds != null && rawFwds.size() > 0) {
            for (Object fwdObj : rawFwds) {
                try {
                    ForwardEndpoint fwd = new ForwardEndpoint((DBObject) fwdObj);
                    fwds.add(fwd);
                } catch (Exception e) {
                    LOG.warn("Skipping forward endpoint in Stream.getForwardedTo(): " + e.getMessage(), e);
                    continue;
                }
            }
        }

        this.forwardedTo = fwds;
        return fwds;
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