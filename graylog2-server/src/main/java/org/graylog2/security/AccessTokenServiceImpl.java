/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.security;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UnwindOptions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.graylog2.users.UserConfiguration;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.PeriodDuration;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Aggregates.lookup;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.orderBy;

/**
 * Provides access to access tokens in the database.
 * <p/>
 * The token value will automatically be encrypted/decrypted when storing/loading the token object from the database.
 * That means the token value is encrypted at rest but the loaded {@link AccessToken} always contains the plain text value.
 */
@Singleton
public class AccessTokenServiceImpl extends PersistedServiceImpl implements AccessTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenServiceImpl.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    private final PaginatedAccessTokenEntityService paginatedAccessTokenEntityService;
    private final AccessTokenCipher cipher;
    private final ClusterConfigService configService;
    private LoadingCache<String, DateTime> lastAccessCache;

    @Inject
    public AccessTokenServiceImpl(MongoConnection mongoConnection, PaginatedAccessTokenEntityService paginatedAccessTokenEntityService, AccessTokenCipher accessTokenCipher, ClusterConfigService configService) {
        super(mongoConnection);
        this.paginatedAccessTokenEntityService = paginatedAccessTokenEntityService;
        this.cipher = accessTokenCipher;
        this.configService = configService;
        setLastAccessCache(30, TimeUnit.SECONDS);

        collection(AccessTokenImpl.class).createIndex(new BasicDBObject(AccessTokenImpl.TOKEN_TYPE, 1));
        // make sure we cannot overwrite an existing access token
        collection(AccessTokenImpl.class).createIndex(new BasicDBObject(AccessTokenImpl.TOKEN, 1), new BasicDBObject("unique", true));
        // Add an index on the expires_at field to speed up the search for expired tokens:
        collection(AccessTokenImpl.class).createIndex(new BasicDBObject(AccessTokenImpl.EXPIRES_AT, 1));
    }

    @Override
    public AccessToken load(String token) {
        DBObject query = new BasicDBObject();
        query.put(AccessTokenImpl.TOKEN, cipher.encrypt(token));
        final List<DBObject> objects = query(AccessTokenImpl.class, query);

        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() > 1) {
            LOG.error("Multiple access tokens found, this is a serious bug.");
            throw new IllegalStateException("Access tokens collection has no unique index!");
        }
        return fromDBObject(objects.get(0));
    }

    @Nullable
    @Override
    public AccessToken loadById(String id) {
        try {
            final DBObject dbObject = get(AccessTokenImpl.class, id);
            if (dbObject != null) {
                return fromDBObject(dbObject);
            }
        } catch (IllegalArgumentException e) {
            // Happens when id is not a valid BSON ObjectId
            LOG.debug("Couldn't load access token", e);
        }
        return null;
    }

    @Override
    public List<AccessToken> loadAll(String username) {
        DBObject query = new BasicDBObject();
        query.put(AccessTokenImpl.USERNAME, username);
        final List<DBObject> objects = query(AccessTokenImpl.class, query);
        return objects.stream()
                .map(this::fromDBObject)
                .collect(Collectors.toList());
    }

    @Override
    public AccessToken create(String username, String name) {
        final PeriodDuration defaultTTL = configService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES).defaultTTLForNewTokens();
        return create(username, name, defaultTTL);
    }

    @Override
    public AccessToken create(String username, String name, PeriodDuration ttl) {
        Map<String, Object> fields = Maps.newHashMap();
        AccessTokenImpl accessToken;
        String id = null;
        int iterations = 0;
        // let's create a unique access token.
        // this loop should never have a collision, but we will try up to 10 times anyway.
        do {
            // 256 bits of randomness should be plenty hard to guess, no?
            final String token = new BigInteger(256, RANDOM).toString(32);
            final DateTime nowUTC = Tools.nowUTC();
            fields.put(AccessTokenImpl.TOKEN, token);
            fields.put(AccessTokenImpl.USERNAME, username);
            fields.put(AccessTokenImpl.NAME, name);
            fields.put(AccessTokenImpl.CREATED_AT, nowUTC);
            // This is kind of ugly, as we're still using joda-time. Once we're with java.time, one can use "nowUtc.plus(ttl)".
            // Until then, we need to add all fields of the period and the duration separately:
            fields.put(AccessTokenImpl.EXPIRES_AT, addTtlPeriodDuration(nowUTC, ttl));
            fields.put(AccessTokenImpl.LAST_ACCESS, Tools.dateTimeFromDouble(0)); // aka never.
            accessToken = new AccessTokenImpl(fields);
            try {
                id = saveWithoutValidation(encrypt(accessToken));
            } catch (MongoException e) {
                // ignore duplicate key errors
                if (!MongoUtils.isDuplicateKeyError(e)) {
                    throw e;
                }
            }
        } while (iterations++ < 10 && id == null);
        if (id == null) {
            throw new IllegalStateException("Could not create unique access token, tried 10 times. This is bad.");
        }
        return accessToken;
    }

    private DateTime addTtlPeriodDuration(DateTime dt, PeriodDuration ttl) {
        final Period p = ttl.getPeriod();
        return dt.plusYears(p.getYears()).plusMonths(p.getMonths()).plusDays(p.getDays()).plus(ttl.getDuration().toMillis());
    }

    @Override
    public DateTime touch(AccessToken accessToken) throws ValidationException {
        try {
            return lastAccessCache.get(accessToken.getId());
        } catch (ExecutionException e) {
            LOG.debug("Ignoring error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String save(AccessToken accessToken) throws ValidationException {
        return super.save(encrypt(accessToken));
    }

    @Override
    public int deleteAllForUser(String username) {
        LOG.debug("Deleting all access tokens of user \"{}\"", username);
        final DBObject query = BasicDBObjectBuilder.start(AccessTokenImpl.USERNAME, username).get();
        final int result = destroy(query, AccessTokenImpl.COLLECTION_NAME);
        LOG.debug("Deleted {} access tokens of user \"{}\"", result, username);
        return result;
    }

    @SuppressWarnings("unchecked")
    private AccessTokenImpl fromDBObject(DBObject dbObject) {
        final Map<String, Object> fields = new HashMap<String, Object>(dbObject.toMap());
        final String ciphertext = (String) fields.get(AccessTokenImpl.TOKEN);
        if (StringUtils.isNotBlank(ciphertext)) {
            fields.put(AccessTokenImpl.TOKEN, cipher.decrypt(ciphertext));
            // The token type field is only used internally for now so we don't want to expose it
            fields.remove(AccessTokenImpl.TOKEN_TYPE);
        }
        final ObjectId id = (ObjectId) dbObject.get(AccessTokenImpl.ID_FIELD);
        return new AccessTokenImpl(id, fields);
    }

    private AccessTokenImpl encrypt(AccessToken token) {
        Map<String, Object> fields = new HashMap<>(token.getFields());
        final String cleartext = (String) fields.get(AccessTokenImpl.TOKEN);
        if (StringUtils.isNotBlank(cleartext)) {
            fields.put(AccessTokenImpl.TOKEN, cipher.encrypt(cleartext));
            // The token type is used to state the algorithm that is used to encrypt the value
            fields.put(AccessTokenImpl.TOKEN_TYPE, AccessTokenImpl.Type.AES_SIV.getIntValue());
        }
        if (token.getId() == null) {
            return new AccessTokenImpl(fields);
        } else {
            return new AccessTokenImpl(new ObjectId(token.getId()), fields);
        }
    }

    @VisibleForTesting
    @Override
    public void setLastAccessCache(long duration, TimeUnit unit) {
        lastAccessCache = CacheBuilder.newBuilder()
                .expireAfterAccess(duration, unit)
                .build(new CacheLoader<>() {
                    @Override
                    public DateTime load(String id) throws Exception {
                        AccessToken accessToken = loadById(id);
                        DateTime now = Tools.nowUTC();
                        if (accessToken != null) {
                            accessToken.getFields().put(AccessTokenImpl.LAST_ACCESS, Tools.nowUTC());
                            LOG.debug("Accesstoken: saving access time");
                            save(accessToken);
                        }
                        return now;
                    }
                });
    }

    @Override
    public PaginatedList<AccessTokenEntity> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, SortOrder order) {
        return this.paginatedAccessTokenEntityService.findPaginated(searchQuery, page, perPage, sortField, order);
    }

    @Override
    public List<ExpiredToken> findExpiredTokens(DateTime expiredBefore) {
        final String join = "userDetails";
        final String joinId = join + "._id";

        final List<Bson> pipeline = List.of(
                //Search fot tokens whose EXPIRES_AT is less than or equal to now:
                match(lte(AccessTokenImpl.EXPIRES_AT, expiredBefore.toDate())),
                // Sort by expiration date:
                sort(orderBy(ascending(AccessTokenImpl.EXPIRES_AT))),
                // Join in the user-collection on the username as "userDetails":
                lookup(UserImpl.COLLECTION_NAME, AccessTokenImpl.USERNAME, UserImpl.USERNAME, join),
                // Have a dedicated Doc for each user-id. In case the user doesn't exist, we still keep the entire entry:
                unwind("$" + join, new UnwindOptions().preserveNullAndEmptyArrays(true)),
                // Load only token-id, expiration date and user-id:
                project(fields(
                        include(AccessTokenImpl.ID_FIELD),
                        include(AccessTokenImpl.NAME),
                        include(AccessTokenImpl.EXPIRES_AT),
                        include(AccessTokenImpl.USERNAME),
                        include(joinId)
                ))
        );

        final MongoCollection<Document> tokenColl = mongoCollection(AccessTokenImpl.class);
        final AggregateIterable<Document> aggregateIt = tokenColl.aggregate(pipeline);
        try (var stream = StreamSupport.stream(aggregateIt.spliterator(), false)) {
            return stream.map(d -> {
                final Optional<Document> userDetails = Optional.ofNullable(d.get(join, Document.class));
                        return new ExpiredToken(
                                d.getObjectId(AccessTokenImpl.ID_FIELD).toString(),
                                d.getString(AccessTokenImpl.NAME),
                                new DateTime(d.getDate(AccessTokenImpl.EXPIRES_AT)).withZone(DateTimeZone.UTC),
                                //Return null for non-existing users, but definitely append the username from the token itself:
                                userDetails.map(ud -> ud.getObjectId("_id").toString()).orElse(null),
                                d.getString(AccessTokenImpl.USERNAME)
                        );
                    }
            ).toList();
        }
    }

    @Override
    public List<ExpiredToken> findOrphanedTokens() {
        final String joinField = "token_username";
        final MongoCollection<Document> tokenCollection = mongoCollection(AccessTokenImpl.class);
        final AggregateIterable<Document> aggregateIt = tokenCollection.aggregate(List.of(
                lookup(UserImpl.COLLECTION_NAME, AccessTokenImpl.USERNAME, UserImpl.USERNAME, joinField),
                match(eq(joinField, new Document("$size", 0))),
                // Load only token-id, expiration date and user-name:
                project(fields(
                        include(AccessTokenImpl.ID_FIELD),
                        include(AccessTokenImpl.NAME),
                        include(AccessTokenImpl.EXPIRES_AT),
                        include(AccessTokenImpl.USERNAME)
                ))
        ));
        try (var stream = StreamSupport.stream(aggregateIt.spliterator(), false)) {
            return stream.map(d ->
                    new ExpiredToken(
                            d.getObjectId(AccessTokenImpl.ID_FIELD).toString(),
                            d.getString(AccessTokenImpl.NAME),
                            new DateTime(d.getDate(AccessTokenImpl.EXPIRES_AT)).withZone(DateTimeZone.UTC),
                            //The user doesn't exist. At least that's what we're looking for.
                            null,
                            d.getString(AccessTokenImpl.USERNAME)
                    )
            ).toList();
        }
    }

    @Override
    public int deleteById(final String id) {
        final DBObject query = new BasicDBObject(AccessTokenImpl.ID_FIELD, new ObjectId(id));
        return destroy(query, AccessTokenImpl.COLLECTION_NAME);
    }
}
