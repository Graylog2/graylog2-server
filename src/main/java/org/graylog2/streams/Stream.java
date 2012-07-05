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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.forwarders.ForwardEndpoint;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
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

    public static List<Stream> fetchAllEnabled() {
        StreamCache streamCache = StreamCache.getInstance();
        if (streamCache.valid()) {
            return streamCache.get();
        }

        List<Stream> streams = Lists.newArrayList();

        DBCollection coll = streamCache.getGraylogServer().getMongoConnection().getDatabase().getCollection("streams");
        DBObject query = new BasicDBObject();
        query.put("disabled", new BasicDBObject("$ne", true));
        DBCursor cur = coll.find(query);

        while (cur.hasNext()) {
            try {
                streams.add(new Stream(cur.next()));
            } catch (Exception e) {
                LOG.warn("Can't fetch stream. Skipping. " + e.getMessage(), e);
            }
        }

        streamCache.set(streams);

        return streams;
    }

    public List<StreamRule> getStreamRules() {
        if (this.streamRules != null) {
            return this.streamRules;
        }

        List<StreamRule> rules = Lists.newArrayList();

        BasicDBList rawRules = (BasicDBList) this.mongoObject.get("streamrules");
        if (rawRules != null && rawRules.size() > 0) {
            for (Object ruleObj : rawRules) {
                try {
                    StreamRule rule = new StreamRule((DBObject) ruleObj);
                    rules.add(rule);
                } catch (Exception e) {
                    LOG.warn("Skipping stream rule in Stream.getStreamRules(): " + e.getMessage(), e);
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

        List<ForwardEndpoint> fwds = Lists.newArrayList();

        BasicDBList rawFwds = (BasicDBList) this.mongoObject.get("forwarders");
        if (rawFwds != null && rawFwds.size() > 0) {
            for (Object fwdObj : rawFwds) {
                try {
                    ForwardEndpoint fwd = new ForwardEndpoint((DBObject) fwdObj);
                    fwds.add(fwd);
                } catch (Exception e) {
                    LOG.warn("Skipping forward endpoint in Stream.getForwardedTo(): " + e.getMessage(), e);
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