/**
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
 */
package org.graylog2.system.activities;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;

import java.util.List;
import java.util.Map;

public class SystemMessageServiceImpl extends PersistedServiceImpl implements SystemMessageService {
    private final int PER_PAGE = 30;

    @Inject
    public SystemMessageServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public List<SystemMessage> all(int page) {
        List<SystemMessage> messages = Lists.newArrayList();

        DBObject sort = new BasicDBObject();
        sort.put("timestamp", -1);

        List<DBObject> results = query(SystemMessageImpl.class, new BasicDBObject(), sort, PER_PAGE, PER_PAGE * page);
        for (DBObject o : results) {
            messages.add(new SystemMessageImpl(new ObjectId(o.get("_id").toString()), o.toMap()));
        }

        return messages;
    }

    @Override
    public long totalCount() {
        return super.totalCount(SystemMessageImpl.class);
    }

    @Override
    public SystemMessage create(Map<String, Object> fields) {
        return new SystemMessageImpl(fields);
    }
}