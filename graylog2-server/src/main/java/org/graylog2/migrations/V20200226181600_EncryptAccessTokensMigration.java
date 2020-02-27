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
package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.security.AccessToken;
import org.graylog2.security.token.AccessTokenCipher;
import org.graylog2.security.token.AccessTokenImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static com.google.common.base.Strings.isNullOrEmpty;

public class V20200226181600_EncryptAccessTokensMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200226181600_EncryptAccessTokensMigration.class);

    private final MongoConnection mongoConnection;
    private final AccessTokenCipher accessTokenCipher;

    @Inject
    public V20200226181600_EncryptAccessTokensMigration(AccessTokenCipher accessTokenCipher, MongoConnection mongoConnection) {
        this.accessTokenCipher = accessTokenCipher;
        this.mongoConnection = mongoConnection;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-02-26T18:16:00Z");
    }

    @Override
    public void upgrade() {
        final MongoCollection<Document> collection = mongoConnection.getMongoDatabase().getCollection(AccessTokenImpl.COLLECTION_NAME);

        // We use the absence of the "token_type" field as an indicator to select access tokens that need to be encrypted
        // If we should change the encryption method in the future, we need to adjust the query
        for (final Document document : collection.find(Filters.exists(AccessTokenImpl.TOKEN_TYPE, false))) {
            final String tokenId = document.getObjectId("_id").toHexString();
            final String tokenName = document.getString(AccessTokenImpl.NAME);
            final String tokenUsername = document.getString(AccessTokenImpl.USERNAME);
            final String tokenValue = document.getString(AccessTokenImpl.TOKEN);

            if (isNullOrEmpty(tokenValue)) {
                LOG.warn("Couldn't encrypt empty value for access token <{}/{}> of user <{}>", tokenId, tokenName, tokenUsername);
                continue;
            }

            final Bson query = Filters.eq("_id", document.getObjectId("_id"));
            final Bson updates = Updates.combine(
                    Updates.set(AccessTokenImpl.TOKEN_TYPE, AccessToken.Type.AESSIV.getIntValue()),
                    Updates.set(AccessTokenImpl.TOKEN, accessTokenCipher.encrypt(tokenValue))
            );

            LOG.info("Encrypting access token <{}/{}> for user <{}>", tokenId, tokenName, tokenUsername);
            final UpdateResult result = collection.updateOne(query, updates);
            if (result.getModifiedCount() != 1) {
                LOG.warn("Expected to modify one access token, but <{}> have been updated", result.getModifiedCount());
            }
        }
    }
}
