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
package org.graylog2.security;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.Map;

@CollectionName(AccessTokenImpl.COLLECTION_NAME)
public class AccessTokenImpl extends PersistedImpl implements AccessToken {
    public enum Type {
        PLAINTEXT(0), AESSIV(1);

        private final int type;

        Type(int type) {
            this.type = type;
        }

        public int getIntValue() {
            return type;
        }
    }

    public static final String COLLECTION_NAME = "access_tokens";
    public static final String USERNAME = "username";
    public static final String TOKEN = "token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String NAME = "NAME";

    public static final String LAST_ACCESS = "last_access";

    public AccessTokenImpl(Map<String, Object> fields) {
        super(fields);
    }

    public AccessTokenImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String, Validator> getValidations() {
        Map<String, Validator> validations = Maps.newHashMap();
        validations.put(USERNAME, new FilledStringValidator());
        validations.put(TOKEN, new FilledStringValidator());
        validations.put(NAME, new FilledStringValidator());
        return validations;
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }

    @Override
    public DateTime getLastAccess() {
        final Object o = fields.get(LAST_ACCESS);
        return (DateTime) o;
    }

    @Override
    public String getUserName() {
        return String.valueOf(fields.get(USERNAME));
    }

    @Override
    public void setUserName(String userName) {
        fields.put(USERNAME, userName);
    }

    @Override
    public String getToken() {
        return String.valueOf(fields.get(TOKEN));
    }

    @Override
    public void setToken(String token) {
        fields.put(TOKEN, token);
        // The token type is used to state the algorithm that is used to encrypt the value
        fields.put(TOKEN_TYPE, Type.AESSIV.getIntValue());
    }

    @Override
    public String getName() {
        return String.valueOf(fields.get(NAME));
    }

    @Override
    public void setName(String name) {
        fields.put(NAME, name);
    }
}
