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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.shiro.subject.Subject;
import org.graylog2.cluster.NodeService;
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
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.ProxiedResource.CallResult;
import org.graylog2.shared.rest.resources.system.RemoteMetricsResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemPluginResource;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;
import org.graylog2.shared.system.stats.SystemStats;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.graylog2.system.stats.ClusterStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.http.GET;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SupportBundleService {
    public static final int LOGFILE_ENUMERATION_RANGE = 5; // how many rotated logs should we look for
    private static final Logger LOG = LoggerFactory.getLogger(SupportBundleService.class);
    public static final String SUPPORT_BUNDLE_DIR_NAME = "support-bundle";
    public static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);
    public static final String BUNDLE_NAME_PREFIX = "graylog-support-bundle";
    public static final String IN_MEMORY_LOGFILE_ID = "memory";
    public static final long LOG_COLLECTION_SIZE_LIMIT = 60 * 1024 * 1024; // Limits how many on-disk logs we collect per node

    private final ExecutorService executor;
    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final Path bundleDir;
    private final ObjectMapper objectMapper;
    private final ClusterStatsService clusterStatsService;
    private final VersionProbe elasticVersionProbe;
    private final List<URI> elasticsearchHosts;
    private final ClusterAdapter searchDbClusterAdapter;


    @Inject
    public SupportBundleService(@Named("proxiedRequestsExecutorService") ExecutorService executor,
                                NodeService nodeService,
                                RemoteInterfaceProvider remoteInterfaceProvider,
                                @Named("data_dir") Path dataDir,
                                ObjectMapperProvider objectMapperProvider,
                                ClusterStatsService clusterStatsService,
                                VersionProbe searchDbProbe,
                                @IndexerHosts List<URI> searchDbHosts,
                                ClusterAdapter searchDbClusterAdapter) {
        this.executor = executor;
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        objectMapper = objectMapperProvider.get();
        bundleDir = dataDir.resolve(SUPPORT_BUNDLE_DIR_NAME);
        this.clusterStatsService = clusterStatsService;
        this.elasticVersionProbe = searchDbProbe;
        this.elasticsearchHosts = searchDbHosts;
        this.searchDbClusterAdapter = searchDbClusterAdapter;
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

            // Fetch from all nodes in parallel
            final List<CompletableFuture<Void>> futures = nodeManifests.entrySet().stream().map(entry ->
                    CompletableFuture.runAsync(() -> fetchNodeInfos(proxiedResourceHelper, entry.getKey(), entry.getValue(), finalSpoolDir), executor)).toList();
            for (CompletableFuture<Void> f : futures) {
                f.get();
            }
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
        try (FileOutputStream clusterJson = new FileOutputStream(tmpDir.resolve("cluster.json").toFile())) {
            final Map<String, CallResult<SystemOverviewResponse>> systemOverview =
                    proxiedResourceHelper.requestOnAllNodes(RemoteSystemResource.class, RemoteSystemResource::system, CALL_TIMEOUT);

            final Map<String, CallResult<SystemJVMResponse>> jvm = proxiedResourceHelper.requestOnAllNodes(
                    RemoteSystemResource.class, RemoteSystemResource::jvm, CALL_TIMEOUT);

            final Map<String, CallResult<SystemProcessBufferDumpResponse>> processBuffer = proxiedResourceHelper.requestOnAllNodes(
                    RemoteSystemResource.class, RemoteSystemResource::processBufferDump, CALL_TIMEOUT);

            final Map<String, CallResult<PluginList>> installedPlugins = proxiedResourceHelper.requestOnAllNodes(
                    RemoteSystemPluginResource.class, RemoteSystemPluginResource::list, CALL_TIMEOUT);

            final Map<String, Object> result = new HashMap<>(
                    Map.of(
                            "manifest", nodeManifests,
                            "cluster_system_overview", stripCallResult(systemOverview),
                            "jvm", stripCallResult(jvm),
                            "process_buffer_dump", stripCallResult(processBuffer),
                            "installed_plugins", stripCallResult(installedPlugins)
                    )
            );
            result.putAll(getClusterInfo());

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(clusterJson, result);
        }
    }

    private Map<String, Object> getClusterInfo() {
        ExecutorService executorService =
                Executors.newFixedThreadPool(3,
                        new ThreadFactoryBuilder().setNameFormat("support-bundle-cluster-info-collector").build());

        final Map<String, Object> clusterInfo = new ConcurrentHashMap<>();
        final Map<String, Object> searchDb = new ConcurrentHashMap<>();

        final CompletableFuture<?> clusterStats = timeLimitedOrErrorString(clusterStatsService::clusterStats,
                executorService).thenAccept(stats -> clusterInfo.put("cluster_stats", stats));

        final CompletableFuture<?> searchDbVersion =
                timeLimitedOrErrorString(() -> elasticVersionProbe.probe(elasticsearchHosts)
                        .map(SearchVersion::toString).orElse("Unknown"), executorService)
                        .thenAccept(version -> searchDb.put("version", version));
        final CompletableFuture<?> searchDbStats = timeLimitedOrErrorString(searchDbClusterAdapter::rawClusterStats,
                executorService).thenAccept(stats -> searchDb.put("stats", stats));

        try {
            CompletableFuture.allOf(clusterStats, searchDbVersion, searchDbStats).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed collecting cluster info", e);
        } finally {
            executorService.shutdownNow();
        }

        clusterInfo.put("search_db", searchDb);
        return clusterInfo;
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
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(Instant.now().toEpochMilli());
    }

    private void writeZipFile(Path tmpDir) throws IOException {
        var zipFile = Path.of("." + BUNDLE_NAME_PREFIX + "-" + nowTimestamp() + ".zip");

        try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(bundleDir.resolve(zipFile).toFile()))) {
            try (final Stream<Path> walk = Files.walk(tmpDir)) {
                walk.filter(p -> !Files.isDirectory(p)).forEach(p -> {
                    var zipEntry = new ZipEntry(tmpDir.relativize(p).toString());
                    try {
                        zipStream.putNextEntry(zipEntry);
                        Files.copy(p, zipStream);
                        zipStream.closeEntry();
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
        Files.move(bundleDir.resolve(zipFile), bundleDir.resolve(Path.of(zipFile.toString().substring(1))));
    }

    private Path prepareBundleSpoolDir() throws IOException {
        final FileAttribute<Set<PosixFilePermission>> userOnlyPermission =
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
        Files.createDirectories(bundleDir, userOnlyPermission);

        return Files.createTempDirectory(bundleDir, ".tmp." + nowTimestamp() + ".", userOnlyPermission);
    }

    private Map<String, SupportBundleNodeManifest> extractManifests(Map<String, CallResult<SupportBundleNodeManifest>> manifestResponse) {
        //noinspection OptionalGetWithoutIsPresent
        return manifestResponse.entrySet().stream().filter(result -> {
            var node = result.getKey();
            var response = result.getValue().response();
            if (response == null || !response.isSuccess() || response.entity().isEmpty()) {
                LOG.warn("Missing SupportBundleNodeManifest for Node <{}>", node);
                return false;
            }
            return true;
        }).collect(Collectors.toMap(Map.Entry::getKey, res -> Objects.requireNonNull(res.getValue().response()).entity().get()));
    }

    private void fetchNodeInfos(ProxiedResourceHelper proxiedResourceHelper, String nodeId, SupportBundleNodeManifest manifest, Path tmpDir) {
        final Path nodeDir = tmpDir.resolve(new SimpleNodeId(nodeId).getShortNodeId());
        var ignored = nodeDir.toFile().mkdirs();

        fetchLogs(proxiedResourceHelper, nodeId, manifest.entries().logfiles(), nodeDir);
        fetchNodeInfo(proxiedResourceHelper, nodeId, nodeDir);
    }

    private void fetchNodeInfo(ProxiedResourceHelper proxiedResourceHelper, String nodeId, Path nodeDir) {
        try (var threadDumpFile = new FileOutputStream(nodeDir.resolve("thread-dump.txt").toFile())) {

            final ProxiedResource.NodeResponse<SystemThreadDumpResponse> dump = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteSystemResource.class, RemoteSystemResource::threadDump, Function.identity(), CALL_TIMEOUT
            );
            if (dump.entity().isPresent()) {
                threadDumpFile.write(dump.entity().get().threadDump().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            LOG.warn("Failed to get threadDump from node <{}>", nodeId, e);
        }

        try (var nodeMetricsFile = new FileOutputStream(nodeDir.resolve("metrics.json").toFile())) {
            final ProxiedResource.NodeResponse<MetricsSummaryResponse> metrics = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteMetricsResource.class, c -> c.byNamespace("org"), Function.identity(), CALL_TIMEOUT
            );
            if (metrics.entity().isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(nodeMetricsFile, metrics.entity().get());
            }
        } catch (Exception e) {
            LOG.warn("Failed to get metrics from node <{}>", nodeId, e);
        }

        try (var systemStatsFile = new FileOutputStream(nodeDir.resolve("system-stats.json").toFile())) {
            final ProxiedResource.NodeResponse<SystemStats> statsResponse = proxiedResourceHelper.doNodeApiCall(nodeId,
                    RemoteSystemStatsResource.class, RemoteSystemStatsResource::systemStats, Function.identity(), CALL_TIMEOUT
            );
            if (statsResponse.entity().isPresent()) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(systemStatsFile, statsResponse.entity().get());
            }
        } catch (Exception e) {
            LOG.warn("Failed to get system stats from node <{}>", nodeId, e);
        }
    }

    @VisibleForTesting
    List<LogFile> applyBundleSizeLogFileLimit(List<LogFile> allLogs) {
        final ImmutableList.Builder<LogFile> truncatedLogFileList = ImmutableList.builder();

        // Always collect the in-memory log and the newest on-disk log file
        // Keep collecting until we pass LOG_COLLECTION_SIZE_LIMIT
        final AtomicBoolean oneFileAdded = new AtomicBoolean(false);
        final AtomicLong collectedSize = new AtomicLong();
        allLogs.stream().sorted(Comparator.comparing(LogFile::lastModified).reversed()).forEach(logFile -> {
            if (logFile.id().equals(IN_MEMORY_LOGFILE_ID)) {
                truncatedLogFileList.add(logFile);
            } else if (!oneFileAdded.get() || collectedSize.get() < LOG_COLLECTION_SIZE_LIMIT) {
                truncatedLogFileList.add(logFile);
                oneFileAdded.set(true);
                collectedSize.addAndGet(logFile.size());
            }
        });
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
                        try (var logFileStream = response.entity().get().byteStream()) {
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
        try (var files = Files.walk(bundleDir)) {
            return files
                    .filter(p -> !Files.isDirectory(p))
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
        } catch (Exception e) {
            outputStream.close();
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

    }

    interface RemoteSystemStatsResource {
        @GET("system/stats")
        Call<SystemStats> systemStats();
    }
}
