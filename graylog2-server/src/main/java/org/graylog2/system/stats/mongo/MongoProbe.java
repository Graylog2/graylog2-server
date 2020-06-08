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
package org.graylog2.system.stats.mongo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import org.graylog2.database.MongoConnection;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class MongoProbe {
    private static final Logger LOG = LoggerFactory.getLogger(MongoProbe.class);

    private final Mongo mongoClient;
    private final DB db;
    private final DB adminDb;
    private final BuildInfo buildInfo;
    private final HostInfo hostInfo;

    @Inject
    public MongoProbe(MongoConnection mongoConnection) {
        this(mongoConnection.connect(), mongoConnection.getDatabase());
    }

    @VisibleForTesting
    MongoProbe(Mongo mongoClient, DB db) {
        this(mongoClient, db, mongoClient.getDB("admin"),
                createBuildInfo(mongoClient.getDB("admin")), createHostInfo(mongoClient.getDB("admin")));
    }

    @VisibleForTesting
    MongoProbe(Mongo mongoClient, DB db, DB adminDB, BuildInfo buildInfo, HostInfo hostInfo) {
        this.mongoClient = checkNotNull(mongoClient);
        this.db = checkNotNull(db);
        this.adminDb = checkNotNull(adminDB);
        this.buildInfo = buildInfo;
        this.hostInfo = hostInfo;
    }

    private static HostInfo createHostInfo(DB adminDb) {
        final HostInfo hostInfo;
        final CommandResult hostInfoResult = adminDb.command("hostInfo");
        if (hostInfoResult.ok()) {
            final BasicDBObject systemMap = (BasicDBObject) hostInfoResult.get("system");
            final HostInfo.System system = HostInfo.System.create(
                    new DateTime(systemMap.getDate("currentTime")),
                    systemMap.getString("hostname"),
                    systemMap.getInt("cpuAddrSize"),
                    systemMap.getLong("memSizeMB"),
                    systemMap.getInt("numCores"),
                    systemMap.getString("cpuArch"),
                    systemMap.getBoolean("numaEnabled")
            );
            final BasicDBObject osMap = (BasicDBObject) hostInfoResult.get("os");
            final HostInfo.Os os = HostInfo.Os.create(
                    osMap.getString("type"),
                    osMap.getString("name"),
                    osMap.getString("version")
            );

            final BasicDBObject extraMap = (BasicDBObject) hostInfoResult.get("extra");
            final HostInfo.Extra extra = HostInfo.Extra.create(
                    extraMap.getString("versionString"),
                    extraMap.getString("libcVersion"),
                    extraMap.getString("kernelVersion"),
                    extraMap.getString("cpuFrequencyMHz"),
                    extraMap.getString("cpuFeatures"),
                    extraMap.getString("scheduler"),
                    extraMap.getLong("pageSize", -1L),
                    extraMap.getLong("numPages", -1L),
                    extraMap.getLong("maxOpenFiles", -1L)
            );

            hostInfo = HostInfo.create(system, os, extra);
        } else {
            LOG.debug("Couldn't retrieve MongoDB hostInfo: {}", hostInfoResult.getErrorMessage());
            hostInfo = null;
        }

        return hostInfo;
    }

    private static BuildInfo createBuildInfo(DB adminDb) {
        final BuildInfo buildInfo;
        final CommandResult buildInfoResult = adminDb.command("buildInfo");
        if (buildInfoResult.ok()) {
            buildInfo = BuildInfo.create(
                    buildInfoResult.getString("version"),
                    buildInfoResult.getString("gitVersion"),
                    buildInfoResult.getString("sysInfo"),
                    buildInfoResult.getString("loaderFlags"),
                    buildInfoResult.getString("compilerFlags"),
                    buildInfoResult.getString("allocator"),
                    (List<Integer>) buildInfoResult.get("versionArray"),
                    buildInfoResult.getString("javascriptEngine"),
                    buildInfoResult.getInt("bits"),
                    buildInfoResult.getBoolean("debug"),
                    buildInfoResult.getLong("maxBsonObjectSize")

            );
        } else {
            LOG.debug("Couldn't retrieve MongoDB buildInfo: {}", buildInfoResult.getErrorMessage());
            buildInfo = null;
        }

        return buildInfo;
    }

    public MongoStats mongoStats() {
        final List<ServerAddress> serverAddresses = mongoClient.getServerAddressList();
        final List<HostAndPort> servers = Lists.newArrayListWithCapacity(serverAddresses.size());
        for (ServerAddress serverAddress : serverAddresses) {
            servers.add(HostAndPort.fromParts(serverAddress.getHost(), serverAddress.getPort()));
        }

        final DatabaseStats dbStats;
        final CommandResult dbStatsResult = db.command("dbStats");
        if (dbStatsResult.ok()) {
            final BasicDBObject extentFreeListMap = (BasicDBObject) dbStatsResult.get("extentFreeList");
            final DatabaseStats.ExtentFreeList extentFreeList;
            if (extentFreeListMap == null) {
                extentFreeList = null;
            } else {
                extentFreeList = DatabaseStats.ExtentFreeList.create(
                        extentFreeListMap.getInt("num"),
                        extentFreeListMap.getInt("totalSize")
                );
            }

            final BasicDBObject dataFileVersionMap = (BasicDBObject) dbStatsResult.get("dataFileVersion");
            final DatabaseStats.DataFileVersion dataFileVersion;
            if (dataFileVersionMap == null) {
                dataFileVersion = null;
            } else {
                dataFileVersion = DatabaseStats.DataFileVersion.create(
                        dataFileVersionMap.getInt("major"),
                        dataFileVersionMap.getInt("minor")
                );
            }

            dbStats = DatabaseStats.create(
                    dbStatsResult.getString("db"),
                    dbStatsResult.getLong("collections"),
                    dbStatsResult.getLong("objects"),
                    dbStatsResult.getDouble("avgObjSize"),
                    dbStatsResult.getLong("dataSize"),
                    dbStatsResult.getLong("storageSize"),
                    dbStatsResult.getLong("numExtents"),
                    dbStatsResult.getLong("indexes"),
                    dbStatsResult.getLong("indexSize"),
                    dbStatsResult.containsField("fileSize") ? dbStatsResult.getLong("fileSize") : null,
                    dbStatsResult.containsField("nsSizeMB") ? dbStatsResult.getLong("nsSizeMB") : null,
                    extentFreeList,
                    dataFileVersion
            );
        } else {
            LOG.debug("Couldn't retrieve MongoDB dbStats: {}", dbStatsResult.getErrorMessage());
            dbStats = null;
        }

        final ServerStatus serverStatus;
        final CommandResult serverStatusResult = adminDb.command("serverStatus");
        if (serverStatusResult.ok()) {
            final BasicDBObject connectionsMap = (BasicDBObject) serverStatusResult.get("connections");
            final ServerStatus.Connections connections = ServerStatus.Connections.create(
                    connectionsMap.getInt("current"),
                    connectionsMap.getInt("available"),
                    connectionsMap.containsField("totalCreated") ? connectionsMap.getLong("totalCreated") : null
            );

            final BasicDBObject networkMap = (BasicDBObject) serverStatusResult.get("network");
            final ServerStatus.Network network = ServerStatus.Network.create(
                    networkMap.getInt("bytesIn"),
                    networkMap.getInt("bytesOut"),
                    networkMap.getInt("numRequests")
            );

            final BasicDBObject memoryMap = (BasicDBObject) serverStatusResult.get("mem");
            final ServerStatus.Memory memory = ServerStatus.Memory.create(
                    memoryMap.getInt("bits"),
                    memoryMap.getInt("resident"),
                    memoryMap.getInt("virtual"),
                    memoryMap.getBoolean("supported"),
                    memoryMap.getInt("mapped", -1),
                    memoryMap.getInt("mappedWithJournal", -1)
            );

            final BasicDBObject storageEngineMap = (BasicDBObject) serverStatusResult.get("storageEngine");
            final ServerStatus.StorageEngine storageEngine;
            if (storageEngineMap == null) {
                storageEngine = ServerStatus.StorageEngine.DEFAULT;
            } else {
                storageEngine = ServerStatus.StorageEngine.create(storageEngineMap.getString("name"));
            }

            final int uptime = serverStatusResult.getInt("uptime", 0);
            serverStatus = ServerStatus.create(
                    serverStatusResult.getString("host"),
                    serverStatusResult.getString("version"),
                    serverStatusResult.getString("process"),
                    serverStatusResult.getLong("pid", 0),
                    uptime,
                    serverStatusResult.getLong("uptimeMillis", uptime * 1000L),
                    serverStatusResult.getInt("uptimeEstimate"),
                    new DateTime(serverStatusResult.getDate("localTime")),
                    connections,
                    network,
                    memory,
                    storageEngine);
        } else {
            LOG.debug("Couldn't retrieve MongoDB serverStatus: {}", serverStatusResult.getErrorMessage());
            serverStatus = null;
        }

        // TODO Collection stats? http://docs.mongodb.org/manual/reference/command/collStats/
        return MongoStats.create(servers, buildInfo, hostInfo, serverStatus, dbStats);
    }
}
