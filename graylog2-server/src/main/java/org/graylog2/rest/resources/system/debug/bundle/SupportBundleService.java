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
package org.graylog2.rest.resources.system.debug.bundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.shiro.subject.Subject;
import org.graylog.security.certutil.KeyStoreDto;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.log4j.MemoryAppender;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.metrics.responses.MetricsSummaryResponse;
import org.graylog2.rest.models.system.plugins.responses.PluginList;
import org.graylog2.rest.models.system.responses.SystemJVMResponse;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.models.system.responses.SystemProcessBufferDumpResponse;
import org.graylog2.rest.models.system.responses.SystemThreadDumpResponse;
import org.graylog2.rest.resources.datanodes.DatanodeResolver;
import org.graylog2.rest.resources.datanodes.DatanodeRestApiProxy;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.ProxiedResource.CallResult;
import org.graylog2.shared.rest.resources.system.RemoteDataNodeStatusResource;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemPluginResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;
import org.graylog2.shared.system.stats.SystemStats;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.VersionProbeFactory;
import org.graylog2.system.stats.ClusterStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.http.GET;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SupportBundleService {
    private static final int LOGFILE_ENUMERATION_RANGE = 5; // how many rotated logs should we look for
    private static final Logger LOG = LoggerFactory.getLogger(SupportBundleService.class);
    private static final String SUPPORT_BUNDLE_DIR_NAME = "support-bundle";
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(30);
    private static final String BUNDLE_NAME_PREFIX = "graylog-support-bundle";
    private static final String IN_MEMORY_LOGFILE_ID = "memory";
    private static final long LOG_COLLECTION_SIZE_LIMIT = 60 * 1024 * 1024; // Limits how many on-disk logs we collect per node
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss", Locale.US).withZone(ZoneOffset.UTC);

    private final ExecutorService executor;
    private final NodeService nodeService;
    private final org.graylog2.cluster.nodes.NodeService<DataNodeDto> datanodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final Path bundleDir;
    private final ObjectMapper objectMapper;
    private final ClusterStatsService clusterStatsService;
    private final VersionProbeFactory versionProbeFactory;
    private final List<URI> elasticsearchHosts;
    private final ClusterAdapter searchDbClusterAdapter;
    private final DatanodeRestApiProxy datanodeProxy;

    @Inject
    public SupportBundleService(@Named("proxiedRequestsExecutorService") ExecutorService executor,
                                NodeService nodeService,
                                org.graylog2.cluster.nodes.NodeService<DataNodeDto> datanodeService,
                                RemoteInterfaceProvider remoteInterfaceProvider,
                                @Named("data_dir") Path dataDir,
                                ObjectMapperProvider objectMapperProvider,
                                ClusterStatsService clusterStatsService,
                                VersionProbeFactory searchDbProbeFactory,
                                @IndexerHosts List<URI> searchDbHosts,
                                ClusterAdapter searchDbClusterAdapter, DatanodeRestApiProxy datanodeProxy) {
        this.executor = executor;
        this.nodeService = nodeService;
        this.datanodeService = datanodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        objectMapper = objectMapperProvider.get();
        bundleDir = dataDir.resolve(SUPPORT_BUNDLE_DIR_NAME);
        this.clusterStatsService = clusterStatsService;
        this.versionProbeFactory = searchDbProbeFactory;
        this.elasticsearchHosts = searchDbHosts;
        this.searchDbClusterAdapter = searchDbClusterAdapter;
        this.datanodeProxy = datanodeProxy;
    }

    public void buildBundle(HttpHeaders httpHeaders, Subject currentSubject) {
        final ProxiedResourceHelper proxiedResourceHelper = new ProxiedResourceHelper(httpHeaders, currentSubject, nodeService, remoteInterfaceProvider, executor);

        final var manifestsResponse = proxiedResourceHelper.requestOnAllNodes(
                RemoteSupportBundleInterface.class,
                RemoteSupportBundleInterface::getNodeManifest, CALL_TIMEOUT);
        final Map<String, SupportBundleNodeManifest> nodeManifests = extractManifests(manifestsResponse);

        Path bundleSpoolDir = null;
        try {
            bundleSpoolDir = prepareBundleSpoolDir();
            final Path finalSpoolDir = bundleSpoolDir; // needed for the lambda
            final Path dataNodeDir = bundleSpoolDir.resolve("datanodes");

            // Fetch from all nodes in parallel, with per-node sub-tasks also running as peers
            final List<CompletableFuture<Void>> futures = Stream.concat(
                    nodeManifests.entrySet().stream().flatMap(entry ->
                            fetchNodeInfosAsync(proxiedResourceHelper, entry.getKey(), entry.getValue(), finalSpoolDir).stream()),
                    datanodeService.allActive().values().stream().map(datanode ->
                            CompletableFuture.runAsync(() -> fetchDataNodeInfos(datanode, dataNodeDir), executor))
            ).toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
            fetchClusterInfos(proxiedResourceHelper, nodeManifests, bundleSpoolDir);
            writeZipFile(bundleSpoolDir);
        } catch (Exception e) {
            LOG.warn("Exception while trying to build support bundle", e);
            throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR, e);
        } finally {
            try {
                if (bundleSpoolDir != null) {
                    FileUtils.deleteDirectory(bundleSpoolDir.toFile());
                }
            } catch (IOException e) {
                LOG.error("Failed to cleanup temp directory <{}>", bundleSpoolDir);
            }
        }
    }

    private void fetchClusterInfos(ProxiedResourceHelper proxiedResourceHelper, Map<String, SupportBundleNodeManifest> nodeManifests, Path tmpDir) throws IOException {
        // requestOnAllNodes submits per-node tasks to `executor` then blocks on Future#get.
        // A separate short-lived orchestration executor runs these blocking fan-outs so they
        // never occupy `executor` slots, keeping `executor` free for the per-node HTTP work.
        // The executor is created here (not as a field) because builds are infrequent and
        // there is no value in holding idle threads between them.
        final ExecutorService orchestrationExecutor = Executors.newFixedThreadPool(4,
                Thread.ofPlatform().daemon().name("support-bundle-orchestration-", 0).factory());
        try {
            final CompletableFuture<Map<String, CallResult<SystemOverviewResponse>>> systemOverviewFuture =
                    CompletableFuture.supplyAsync(() -> proxiedResourceHelper.requestOnAllNodes(
                            RemoteSystemResource.class, RemoteSystemResource::system, CALL_TIMEOUT), orchestrationExecutor);

            final CompletableFuture<Map<String, CallResult<SystemJVMResponse>>> jvmFuture =
                    CompletableFuture.supplyAsync(() -> proxiedResourceHelper.requestOnAllNodes(
                            RemoteSystemResource.class, RemoteSystemResource::jvm, CALL_TIMEOUT), orchestrationExecutor);

            final CompletableFuture<Map<String, CallResult<SystemProcessBufferDumpResponse>>> processBufferFuture =
                    CompletableFuture.supplyAsync(() -> proxiedResourceHelper.requestOnAllNodes(
                            RemoteSystemResource.class, RemoteSystemResource::processBufferDump, CALL_TIMEOUT), orchestrationExecutor);

            final CompletableFuture<Map<String, CallResult<PluginList>>> installedPluginsFuture =
                    CompletableFuture.supplyAsync(() -> proxiedResourceHelper.requestOnAllNodes(
                            RemoteSystemPluginResource.class, RemoteSystemPluginResource::list, CALL_TIMEOUT), orchestrationExecutor);

            // These submit leaf tasks to `executor` without blocking on sub-tasks — no nesting risk.
            final CompletableFuture<Object> clusterStatsFuture =
                    timeLimitedOrErrorString(clusterStatsService::clusterStats, executor);

            final CompletableFuture<Object> searchDbVersionFuture =
                    timeLimitedOrErrorString(() -> versionProbeFactory.createDefault().probe(elasticsearchHosts)
                            .map(SearchVersion::toString).orElse("Unknown"), executor);

            final CompletableFuture<Object> searchDbStatsFuture =
                    timeLimitedOrErrorString(searchDbClusterAdapter::rawClusterStats, executor);

            final CompletableFuture<Map<String, Object>> datanodeInfoFuture =
                    CompletableFuture.supplyAsync(this::getDatanodeInfo, executor);

            try {
                CompletableFuture.allOf(systemOverviewFuture, jvmFuture, processBufferFuture, installedPluginsFuture,
                        clusterStatsFuture, searchDbVersionFuture, searchDbStatsFuture, datanodeInfoFuture).get();
            } catch (Exception e) {
                throw new RuntimeException("Failed collecting cluster infos", e);
            }

            final Map<String, Object> searchDb = Map.of(
                    "version", searchDbVersionFuture.join(),
                    "stats", searchDbStatsFuture.join()
            );
            final Map<String, Object> clusterInfo = Map.of(
                    "cluster_stats", clusterStatsFuture.join(),
                    "search_db", searchDb
            );

            try (FileOutputStream clusterJson = new FileOutputStream(tmpDir.resolve("cluster.json").toFile())) {
                final Map<String, Object> result = new HashMap<>(
                        Map.of(
                                "manifest", nodeManifests,
                                "cluster_system_overview", stripCallResult(systemOverviewFuture.join()),
                                "jvm", stripCallResult(jvmFuture.join()),
                                "process_buffer_dump", stripCallResult(processBufferFuture.join()),
                                "installed_plugins", stripCallResult(installedPluginsFuture.join())
                        )
                );
                result.putAll(clusterInfo);
                result.putAll(datanodeInfoFuture.join());

                objectMapper.writerWithDefaultPrettyPrinter().writeValue(clusterJson, result);
            }
        } finally {
            // All futures are complete at this point — shutdownNow has nothing left to interrupt.
            orchestrationExecutor.shutdownNow();
        }
    }

    private Map<String, Object> getDatanodeInfo() {
        Map<String, DataNodeDto> configuredDatanodes = datanodeService.allActive().values().stream()
                .collect(Collectors.toMap(DataNodeDto::getHostname, d -> d));
        Map<String, JsonNode> datanodeStatus = datanodeProxy.remoteInterface(DatanodeResolver.ALL_NODES_KEYWORD, RemoteDataNodeStatusResource.class, RemoteDataNodeStatusResource::status);
        return Map.of("datanodes", Map.of("configured", configuredDatanodes, "running", datanodeStatus));
    }

    private CompletableFuture<Object> timeLimitedOrErrorString(Supplier<Object> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(supplier, executor)
                .exceptionally(e -> Optional.ofNullable(e.getLocalizedMessage()).orElse(e.getClass().getSimpleName()))
                .completeOnTimeout("Timeout after " + CALL_TIMEOUT + "!", CALL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private <T> Map<String, T> stripCallResult(Map<String, CallResult<T>> input) {
        return input.entrySet().stream()
                .filter(e -> e.getValue().response() != null && e.getValue().response().entity().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().response().entity().get()));
    }

    private String nowTimestamp() {
        return TIMESTAMP_FORMAT.format(Instant.now());
    }

    private void writeZipFile(Path tmpDir) throws IOException {
        var zipFile = Files.createFile(
                bundleDir.resolve(Path.of("." + BUNDLE_NAME_PREFIX + "-" + nowTimestamp() + ".zip")),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
        );

        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            try (final Stream<Path> walk = Files.walk(tmpDir)) {
                walk.filter(p -> !Files.isDirectory(p)).forEach(p -> {
                    var zipEntry = new ZipEntry(tmpDir.relativize(p).toString());
                    try {
                        zipStream.putNextEntry(zipEntry);
                        try {
                            Files.copy(p, zipStream);
                        } finally {
                            zipStream.closeEntry();
                        }
                    } catch (IOException e) {
                        LOG.warn("Failure while creating ZipEntry <{}>", zipEntry, e);
                    }
                });
            }
        } catch (Exception e) {
            Files.delete(zipFile);
            LOG.warn("Failed to create zipfile <{}>", zipFile, e);
            throw e;
        }
        Files.move(zipFile, bundleDir.resolve(Path.of(zipFile.getFileName().toString().substring(1))));
    }

    private Path prepareBundleSpoolDir() throws IOException {
        final FileAttribute<Set<PosixFilePermission>> userOnlyPermission =
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
        Files.createDirectories(bundleDir, userOnlyPermission);

        return Files.createTempDirectory(bundleDir, ".tmp." + nowTimestamp() + ".", userOnlyPermission);
    }

    private Map<String, SupportBundleNodeManifest> extractManifests(Map<String, CallResult<SupportBundleNodeManifest>> manifestResponse) {
        return manifestResponse.entrySet().stream().filter(result -> {
            var node = result.getKey();
            var response = result.getValue().response();
            if (response == null || !response.isSuccess() || response.entity().isEmpty()) {
                LOG.warn("Missing SupportBundleNodeManifest for Node <{}>", node);
                return false;
            }
            return true;
        }).collect(Collectors.toMap(Map.Entry::getKey, res -> Objects.requireNonNull(res.getValue().response()).entity().orElseThrow()));
    }

    private List<CompletableFuture<Void>> fetchNodeInfosAsync(ProxiedResourceHelper proxiedResourceHelper, String nodeId, SupportBundleNodeManifest manifest, Path tmpDir) {
        final Path nodeDir = tmpDir.resolve(new SimpleNodeId(nodeId).getShortNodeId());
        var ignored = nodeDir.toFile().mkdirs();

        return List.of(
                CompletableFuture.runAsync(() -> fetchLogs(proxiedResourceHelper, nodeId, manifest.entries().logfiles(), nodeDir), executor),
                CompletableFuture.runAsync(() -> fetchThreadDump(proxiedResourceHelper, nodeId, nodeDir), executor),
                CompletableFuture.runAsync(() -> fetchMetrics(proxiedResourceHelper, nodeId, nodeDir), executor),
                CompletableFuture.runAsync(() -> fetchSystemStats(proxiedResourceHelper, nodeId, nodeDir), executor),
                CompletableFuture.runAsync(() -> fetchNodeCertificates(proxiedResourceHelper, nodeId, nodeDir), executor)
        );
    }

    private void fetchThreadDump(ProxiedResourceHelper proxiedResourceHelper, String nodeId, Path nodeDir) {
        try (var threadDumpFile = new FileOutputStream(nodeDir.resolve("thread-dump.txt").toFile())) {
            final ProxiedResource.NodeResponse<SystemThreadDumpResponse> dump = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteSystemResource.class, RemoteSystemResource::threadDump, Function.identity(), CALL_TIMEOUT);
            if (dump.entity().isPresent()) {
                threadDumpFile.write(dump.entity().get().threadDump().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            LOG.warn("Failed to get threadDump from node <{}>", nodeId, e);
        }
    }

    private void fetchMetrics(ProxiedResourceHelper proxiedResourceHelper, String nodeId, Path nodeDir) {
        try (var nodeMetricsFile = new FileOutputStream(nodeDir.resolve("metrics.json").toFile())) {
            final ProxiedResource.NodeResponse<MetricsSummaryResponse> metrics = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteMetricsResource.class, c -> c.byNamespace("org"), Function.identity(), CALL_TIMEOUT);
            if (metrics.entity().isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(nodeMetricsFile, metrics.entity().get());
            }
        } catch (Exception e) {
            LOG.warn("Failed to get metrics from node <{}>", nodeId, e);
        }
    }

    private void fetchSystemStats(ProxiedResourceHelper proxiedResourceHelper, String nodeId, Path nodeDir) {
        try (var systemStatsFile = new FileOutputStream(nodeDir.resolve("system-stats.json").toFile())) {
            final ProxiedResource.NodeResponse<SystemStats> statsResponse = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteSystemStatsResource.class, RemoteSystemStatsResource::systemStats, Function.identity(), CALL_TIMEOUT);
            if (statsResponse.entity().isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(systemStatsFile, statsResponse.entity().get());
            }
        } catch (Exception e) {
            LOG.warn("Failed to get system stats from node <{}>", nodeId, e);
        }
    }

    private void fetchNodeCertificates(ProxiedResourceHelper proxiedResourceHelper, String nodeId, Path nodeDir) {
        try (var certificatesFile = new FileOutputStream(nodeDir.resolve("certificates.json").toFile())) {
            final ProxiedResource.NodeResponse<Map<String, KeyStoreDto>> certificatesResponse = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteCertificatesResource.class, RemoteCertificatesResource::certificates, Function.identity(), CALL_TIMEOUT);
            if (certificatesResponse.entity().isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(certificatesFile, certificatesResponse.entity().get());
            }
        } catch (Exception e) {
            LOG.warn("Failed to get certificates from node <{}>", nodeId, e);
        }
    }


    private void fetchDataNodeInfos(DataNodeDto datanode, Path dataNodeDir) {
        final Path nodeDir = dataNodeDir.resolve(Objects.requireNonNull(datanode.getHostname()));
        var ignored = nodeDir.toFile().mkdirs();

        getProxiedLog(datanode, nodeDir, "datanode.log", RemoteDataNodeStatusResource::datanodeInternalLogs);
        try (var certificatesFile = new FileOutputStream(nodeDir.resolve("certificates.json").toFile())) {
            Map<String, Map<String, KeyStoreDto>> certificates = datanodeProxy.remoteInterface(datanode.getHostname(), RemoteCertificatesResource.class, RemoteCertificatesResource::certificates);
            if (certificates.containsKey(datanode.getHostname())) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(certificatesFile, certificates.get(datanode.getHostname()));
            }
        } catch (Exception e) {
            LOG.warn("Failed to get certificates from data node <{}>", datanode.getHostname(), e);
        }
    }

    private void getProxiedLog(DataNodeDto datanode, Path nodeDir, String logfile, Function<RemoteDataNodeStatusResource, Call<ResponseBody>> function) {
        try (var opensearchLog = new FileOutputStream(nodeDir.resolve(logfile).toFile())) {
            Map<String, ResponseBody> opensearchOut = datanodeProxy.remoteInterface(datanode.getHostname(), RemoteDataNodeStatusResource.class, function);
            if (opensearchOut.containsKey(datanode.getHostname())) {
                try (final var logStream = opensearchOut.get(datanode.getHostname()).byteStream()) {
                    logStream.transferTo(opensearchLog);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to get logs from data node <{}>", datanode.getHostname(), e);
        }
    }

    @VisibleForTesting
    List<LogFile> applyBundleSizeLogFileLimit(List<LogFile> allLogs) {
        final ImmutableList.Builder<LogFile> truncatedLogFileList = ImmutableList.builder();

        // Always collect the in-memory log and the newest on-disk log file.
        // Keep collecting until we pass LOG_COLLECTION_SIZE_LIMIT.
        boolean oneFileAdded = false;
        long collectedSize = 0;
        for (final LogFile logFile : allLogs.stream().sorted(Comparator.comparing(LogFile::lastModified).reversed()).toList()) {
            if (logFile.id().equals(IN_MEMORY_LOGFILE_ID)) {
                truncatedLogFileList.add(logFile);
            } else if (!oneFileAdded || collectedSize < LOG_COLLECTION_SIZE_LIMIT) {
                truncatedLogFileList.add(logFile);
                oneFileAdded = true;
                collectedSize += logFile.size();
            }
        }
        return truncatedLogFileList.build();
    }

    private void fetchLogs(ProxiedResourceHelper proxiedResourceHelper, String nodeId, List<LogFile> logFiles, Path nodeDir) {
        final Path logDir = nodeDir.resolve("logs");
        var ignored = logDir.toFile().mkdirs();

        applyBundleSizeLogFileLimit(logFiles).forEach(logFile -> {
            try {
                final ProxiedResource.NodeResponse<ResponseBody> response = proxiedResourceHelper.doNodeApiCall(nodeId,
                        RemoteSupportBundleInterface.class, f -> f.getLogFile(logFile.id()), Function.identity(), CALL_TIMEOUT);

                if (response.entity().isPresent()) {
                    final String logName = Path.of(logFile.name()).getFileName().toString();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(logDir.resolve(logName).toFile())) {
                        try (final var logFileStream = response.entity().get().byteStream()) {
                            logFileStream.transferTo(fileOutputStream);
                        }
                    }
                } else {
                    LOG.warn("Failed to fetch logfile <{}> from node <{}>: Empty response", logFile.name(), nodeId);
                }
            } catch (IOException e) {
                LOG.warn("Failed to fetch logfile <{}> from node <{}>", logFile.name(), nodeId, e);
            }
        });
    }

    public SupportBundleNodeManifest getManifest() {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();

        final ImmutableList.Builder<LogFile> logFiles = ImmutableList.builder();

        getFileAppenders(config).forEach(fileAppender -> getRollingFileLogs(fileAppender).forEach(logFiles::add));

        final Optional<MemoryAppender> memAppender = getMemoryAppender(config);
        memAppender.ifPresent(memoryAppender -> getMemLogFiles(memoryAppender).forEach(logFiles::add));

        return new SupportBundleNodeManifest(new BundleEntries(logFiles.build()));
    }

    private static List<RollingFileAppender> getFileAppenders(Configuration config) {
        return config.getAppenders().values().stream().filter(RollingFileAppender.class::isInstance).map(RollingFileAppender.class::cast).toList();
    }

    private static Optional<MemoryAppender> getMemoryAppender(Configuration config) {
        return config.getAppenders().values().stream().filter(MemoryAppender.class::isInstance).map(MemoryAppender.class::cast).findFirst();
    }

    private List<LogFile> getMemLogFiles(MemoryAppender memAppender) {
        try {
            final long logsSize = memAppender.getLogsSize();
            if (logsSize == 0) {
                return List.of();
            }
            return List.of(new LogFile(IN_MEMORY_LOGFILE_ID, "server.mem.log", logsSize, Instant.now()));
        } catch (Exception e) {
            LOG.warn("Failed to get logs from MemoryAppender <{}>", memAppender.getName(), e);
            return List.of();
        }
    }

    private List<LogFile> getRollingFileLogs(RollingFileAppender rollingFileAppender) {
        final String filePattern = rollingFileAppender.getFilePattern();
        final String baseFileName = rollingFileAppender.getFileName();

        final ImmutableList.Builder<LogFile> logFiles = ImmutableList.builder();

        // The current open uncompressed logfile
        buildLogFile("0", baseFileName).ifPresent(logFiles::add);

        // TODO support filePatterns with https://logging.apache.org/log4j/2.x/manual/lookups.html#DateLookup
        // TODO support filePatterns with SimpleDateFormat
        // e.g: filePattern="logs/$${date:yyyy-MM}/app-%d{MM-dd-yyyy}-%i.log.gz"
        var regex = f("^%s\\.%%i\\.gz", baseFileName);
        if (filePattern.matches(regex)) {
            final String formatString = filePattern.replace("%i", "%d");
            IntStream.range(1, LOGFILE_ENUMERATION_RANGE).forEach(i -> {
                var file = f(formatString, i);
                buildLogFile(String.valueOf(i), file).ifPresent(logFiles::add);
            });
        }
        return logFiles.build();
    }

    private Optional<LogFile> buildLogFile(String id, String fileName) {
        try {
            final Path filePath = Path.of(fileName);
            final long size = Files.size(filePath);
            final FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            return Optional.of(new LogFile(id, fileName, size, lastModifiedTime.toInstant()));
        } catch (NoSuchFileException ignored) {
            return Optional.empty();
        } catch (IOException e) {
            LOG.warn("Failed to read logfile <{}>", fileName, e);
            return Optional.empty();
        }
    }

    public void loadLogFileStream(LogFile logFile, OutputStream outputStream) throws IOException {
        if (logFile.id().equals(IN_MEMORY_LOGFILE_ID)) {
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            final Configuration config = context.getConfiguration();
            final Optional<MemoryAppender> memAppender = getMemoryAppender(config);

            if (memAppender.isEmpty()) {
                throw new NotFoundException();
            }
            memAppender.get().streamFormattedLogMessages(outputStream, 0);

        } else {
            Files.copy(Path.of(logFile.name()), outputStream);
        }
    }

    public List<BundleFile> listBundles() {
        try (var files = Files.list(bundleDir)) {
            return files
                    .filter(p -> p.getFileName().toString().startsWith(BUNDLE_NAME_PREFIX))
                    .map(f -> {
                        try {
                            return new BundleFile(f.getFileName().toString(), Files.size(f));
                        } catch (IOException e) {
                            LOG.warn("Exception while trying to list support bundles", e);
                            throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR, e);
                        }
                    })
                    .sorted(Comparator.comparing(BundleFile::fileName).reversed())
                    .collect(Collectors.toList());
        } catch (NoSuchFileException ignored) {
            return List.of();
        } catch (IOException e) {
            LOG.warn("Exception while trying to list support bundles", e);
            throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

    public void downloadBundle(String filename, OutputStream outputStream) throws IOException {
        ensureFileWithinBundleDir(bundleDir, filename);

        try {
            final Path filePath = bundleDir.resolve(filename);
            Files.copy(filePath, outputStream);
        } catch (NoSuchFileException e) {
            throw new NotFoundException(e);
        }
    }

    @VisibleForTesting
    void ensureFileWithinBundleDir(Path bundleDir, String filename) {
        if (!bundleDir.resolve(filename).toAbsolutePath().normalize().startsWith(bundleDir.toAbsolutePath().normalize())) {
            throw new NotFoundException();
        }
    }

    public void deleteBundle(String filename) throws IOException {
        ensureFileWithinBundleDir(bundleDir, filename);
        final Path filePath = bundleDir.resolve(filename);
        Files.delete(filePath);
    }

    static class ProxiedResourceHelper extends ProxiedResource {
        private final Subject currentSubject;

        protected ProxiedResourceHelper(HttpHeaders httpHeaders, Subject currentSubject, NodeService nodeService,
                                        RemoteInterfaceProvider remoteInterfaceProvider, ExecutorService executorService) {
            super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
            this.currentSubject = currentSubject;
        }

        @Override
        protected Subject getSubject() {
            return currentSubject;
        }

        // The following overrides exist solely to widen visibility from protected to
        // package-private so that SupportBundleService can call them directly.

        @Override
        protected <RemoteInterfaceType, RemoteCallResponseType, FinalResponseType> NodeResponse<FinalResponseType> doNodeApiCall(
                String nodeId, Class<RemoteInterfaceType> interfaceClass, Function<RemoteInterfaceType,
                Call<RemoteCallResponseType>> remoteInterfaceFunction, Function<RemoteCallResponseType, FinalResponseType> transformer,
                Duration timeout) throws IOException {
            return super.doNodeApiCall(nodeId, interfaceClass, remoteInterfaceFunction, transformer, timeout);
        }

        @Override
        protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, CallResult<RemoteCallResponseType>> requestOnAllNodes(
                Class<RemoteInterfaceType> interfaceClass, Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn, Duration timeout) {
            return super.requestOnAllNodes(interfaceClass, fn, timeout);
        }

        @Override
        protected <RemoteInterfaceType, RemoteCallResponseType> NodeResponse<RemoteCallResponseType> requestOnLeader(
                Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn, Class<RemoteInterfaceType> interfaceClass, Duration timeout) throws IOException {
            return super.requestOnLeader(fn, interfaceClass, timeout);
        }
    }

    interface RemoteSystemStatsResource {
        @GET("system/stats")
        Call<SystemStats> systemStats();

    }

    interface RemoteCertificatesResource {
        @GET("certificates")
        Call<Map<String, KeyStoreDto>> certificates();

    }
}
