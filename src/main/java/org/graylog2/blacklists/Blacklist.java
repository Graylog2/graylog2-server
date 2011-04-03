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

package org.graylog2.blacklists;

import com.mongodb.*;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.graylog2.Log;
import org.graylog2.database.MongoConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Blacklist.java: Mar 30, 2011 10:05:34 PM
 *
 * Representing a blacklist stored in MongoDB.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Blacklist {

    private static final Logger LOG = Logger.getLogger(Blacklist.class);

    private ObjectId id = null;
    private String title = null;
    private List<BlacklistRule> rules = null;

    private DBObject mongoObject = null;

    public Blacklist(DBObject blacklist) {
        this.id = (ObjectId) blacklist.get("_id");
        this.title = (String) blacklist.get("title");

        this.mongoObject = blacklist;
    }

    public static ArrayList<Blacklist> fetchAll() throws Exception {
        if (BlacklistCache.getInstance().valid()) {
            return BlacklistCache.getInstance().get();
        }

        ArrayList<Blacklist> blacklists = new ArrayList<Blacklist>();

        DBCollection coll = MongoConnection.getInstance().getDatabase().getCollection("blacklists");
        DBCursor cur = coll.find(new BasicDBObject());

        while (cur.hasNext()) {
            try {
                blacklists.add(new Blacklist(cur.next()));
            } catch (Exception e) {
                LOG.warn("Can't fetch blacklist. Skipping. " + e.getMessage(), e);
            }
        }

        BlacklistCache.getInstance().set(blacklists);

        return blacklists;
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

    /**
     * @return the rules
     */
    public List<BlacklistRule> getRules() {
        if (this.rules != null) {
            return this.rules;
        }

        ArrayList<BlacklistRule> tempRules = new ArrayList<BlacklistRule>();

        BasicDBList rawRules = (BasicDBList) this.mongoObject.get("blacklisted_terms");
        if (rawRules != null && rawRules.size() > 0) {
            for (Object ruleObj : rawRules) {
                try {
                    BlacklistRule rule = new BlacklistRule((DBObject) ruleObj);
                    tempRules.add(rule);
                } catch (Exception e) {
                    LOG.warn("Skipping rule in Blacklist.getRules(): " + e.getMessage(), e);
                }
            }
        }

        this.rules = tempRules;
        return tempRules;
    }

}