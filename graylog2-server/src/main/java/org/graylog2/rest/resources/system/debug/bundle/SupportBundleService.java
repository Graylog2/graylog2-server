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
import com.google.common.collect.ImmutableList;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.shiro.subject.Subject;
import org.graylog2.cluster.NodeService;
import org.graylog2.log4j.MemoryAppender;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.models.system.responses.SystemJVMResponse;
import org.graylog2.rest.models.system.responses.SystemOverviewResponse;
import org.graylog2.rest.models.system.responses.SystemProcessBufferDumpResponse;
import org.graylog2.rest.models.system.responses.SystemThreadDumpResponse;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import org.graylog2.shared.rest.resources.ProxiedResource.CallResult;
import org.graylog2.shared.rest.resources.system.RemoteSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SupportBundleService {
    private static final Logger LOG = LoggerFactory.getLogger(SupportBundleService.class);
    public static final String SUPPORT_BUNDLE_DIR_NAME = "support-bundle";
    public static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);
    public static final String BUNDLE_NAME_PREFIX = "graylog-support-bundle";

    private final ExecutorService executor;
    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final Path bundleDir;
    private final ObjectMapper objectMapper;

    @Inject
    public SupportBundleService(@Named("daemonScheduler") ScheduledExecutorService executor, // TODO what is the right ExecutorService?
                                NodeService nodeService,
                                RemoteInterfaceProvider remoteInterfaceProvider,
                                @Named("data_dir") Path dataDir,
                                ObjectMapperProvider objectMapperProvider) {
        this.executor = executor;
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        objectMapper = objectMapperProvider.get();
        bundleDir = dataDir.resolve(SUPPORT_BUNDLE_DIR_NAME);
    }

    public void buildBundle(HttpHeaders httpHeaders, Subject currentSubject) {
        final ProxiedResourceHelper proxiedResourceHelper = new ProxiedResourceHelper(httpHeaders, currentSubject, nodeService, remoteInterfaceProvider, executor);

        final var manifestsResponse = proxiedResourceHelper.requestOnAllNodes(
                proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSupportBundleInterface.class),
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
            throw new BadRequestException(e);
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
                    proxiedResourceHelper.requestOnAllNodes(
                            proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSystemResource.class),
                            RemoteSystemResource::system, CALL_TIMEOUT);

            final Map<String, CallResult<SystemJVMResponse>> jvm = proxiedResourceHelper.requestOnAllNodes(
                    proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSystemResource.class),
                    RemoteSystemResource::jvm, CALL_TIMEOUT);

            final Map<String, CallResult<SystemProcessBufferDumpResponse>> processBuffer = proxiedResourceHelper.requestOnAllNodes(
                    proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSystemResource.class),
                    RemoteSystemResource::processBufferDump, CALL_TIMEOUT);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(clusterJson,
                    Map.of(
                            "manifest", nodeManifests,
                            "cluster_system_overview", stripCallResult(systemOverview),
                            "jvm", stripCallResult(jvm),
                            "process_buffer_dump", stripCallResult(processBuffer)
                    )
            );
        }
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
                    proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSystemResource.class),
                    RemoteSystemResource::threadDump, Function.identity(), CALL_TIMEOUT.toMillis()
            );
            if (dump.entity().isPresent()) {
                threadDumpFile.write(dump.entity().get().threadDump().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            LOG.warn("Failed to get threadDump from node <{}>", nodeId, e);
        }
    }

    private void fetchLogs(ProxiedResourceHelper proxiedResourceHelper, String nodeId, List<LogFile> logFiles, Path nodeDir) {
        final Path logDir = nodeDir.resolve("logs");
        var ignored = logDir.toFile().mkdirs();

        logFiles.forEach(logFile -> {
            try {
                final ProxiedResource.NodeResponse<ResponseBody> response = proxiedResourceHelper.doNodeApiCall(nodeId,
                        proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSupportBundleInterface.class),
                        f -> f.getLogFile(logFile.id()), Function.identity(), CALL_TIMEOUT.toMillis());

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
            memAppender.getLogMessages(1);
            return List.of(new LogFile("memory", memAppender.getName() + ".log", -1, Instant.now()));
        } catch (Exception e) {
            LOG.warn("Failed to get logs from MemoryAppender <{}>", memAppender.getName(), e);
            return List.of();
        }
    }

    private List<LogFile> getRollingFileLogs(RollingFileAppender rollingFileAppender) {
        final String filePattern = rollingFileAppender.getFilePattern();
        final String baseFileName = rollingFileAppender.getFileName();

        final ImmutableList.Builder<LogFile> logFiles = ImmutableList.builder();

        buildLogFile("0", rollingFileAppender.getFileName()).ifPresent(logFiles::add);

        // TODO support filePatterns with https://logging.apache.org/log4j/2.x/manual/lookups.html#DateLookup
        // TODO support filePatterns with SimpleDateFormat
        // e.g: filePattern="logs/$${date:yyyy-MM}/app-%d{MM-dd-yyyy}-%i.log.gz"
        var regex = f("^%s\\.%%i\\.gz", baseFileName);
        if (filePattern.matches(regex)) {
            final String formatString = filePattern.replace("%i", "%d");
            IntStream.range(1, 5).forEach(i -> {
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
        if (logFile.id().equals("memory")) {
            final LoggerContext context = (LoggerContext) LogManager.getContext(false);
            final Configuration config = context.getConfiguration();
            final Optional<MemoryAppender> memAppender = getMemoryAppender(config);

            if (memAppender.isEmpty()) {
                throw new NotFoundException();
            }
            memAppender.get().streamFormattedLogMessages(outputStream);

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
                            throw new BadRequestException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("Exception while trying to list support bundles", e);
            throw new BadRequestException(e);
        }
    }

    public void downloadBundle(String filename, OutputStream outputStream) throws IOException {
        ensureFileWithinBundleDir(filename);

        try {
            final Path filePath = bundleDir.resolve(filename);
            Files.copy(filePath, outputStream);
        } catch (NoSuchFileException e) {
            throw new NotFoundException(e);
        } catch (Exception e) {
            outputStream.close();
        }
    }

    private void ensureFileWithinBundleDir(String filename) throws IOException {
        if (!bundleDir.resolve(filename).toFile().getCanonicalPath().startsWith(bundleDir.toFile().getCanonicalPath())) {
            throw new NotFoundException();
        }
    }

    public void deleteBundle(String filename) throws IOException {
        ensureFileWithinBundleDir(filename);
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
                String nodeId, Function<String, Optional<RemoteInterfaceType>> remoteInterfaceProvider, Function<RemoteInterfaceType,
                Call<RemoteCallResponseType>> remoteInterfaceFunction, Function<RemoteCallResponseType, FinalResponseType> transformer,
                long callTimeoutMs) throws IOException {
            return super.doNodeApiCall(nodeId, remoteInterfaceProvider, remoteInterfaceFunction, transformer, callTimeoutMs);
        }

        @Override
        protected <RemoteInterfaceType, RemoteCallResponseType> Map<String, CallResult<RemoteCallResponseType>> requestOnAllNodes(
                Function<String, Optional<RemoteInterfaceType>> interfaceProvider, Function<RemoteInterfaceType, Call<RemoteCallResponseType>> fn, Duration timeout) {
            return super.requestOnAllNodes(interfaceProvider, fn, timeout);
        }

        @Override
        protected <RemoteInterfaceType> Function<String, Optional<RemoteInterfaceType>> createRemoteInterfaceProvider(Class<RemoteInterfaceType> interfaceClass) {
            return super.createRemoteInterfaceProvider(interfaceClass);
        }
    }
}
