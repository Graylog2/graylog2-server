/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.periodical;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationImpl;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This periodical migrates alert callbacks created on version <= 1.0, which contained two ID fields: `_id` and `id`,
 * both with different types, and confused MongoJack when loading them. Here we remove the `id` field, which
 * is the string representation of `_id`, the actual object id.
 *
 * See https://github.com/Graylog2/graylog2-server/issues/1428 for more details.
 *
 */
public class AlarmCallbacksMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmCallbacksMigrationPeriodical.class);

    private final DBCollection dbCollection;

    @Inject
    public AlarmCallbacksMigrationPeriodical(MongoConnection mongoConnection) {
        final String collectionName = AlarmCallbackConfigurationImpl.class.getAnnotation(CollectionName.class).value();
        this.dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
    }

    @Override
    public void doRun() {
        LOG.debug("Starting alarm callbacks migration");

        final DBObject selection = QueryBuilder.start("id").exists(true).get();
        final DBObject modifications = new BasicDBObject("$unset", new BasicDBObject("id", ""));
        this.dbCollection.updateMulti(selection, modifications);

        LOG.debug("Done with alarm callbacks migration");
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
