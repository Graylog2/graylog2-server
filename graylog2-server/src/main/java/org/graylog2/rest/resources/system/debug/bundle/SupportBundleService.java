package org.graylog2.rest.resources.system.debug.bundle;

import com.google.common.collect.ImmutableList;
import okhttp3.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.shiro.subject.Subject;
import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.rest.resources.ProxiedResource;
import retrofit2.Call;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.graylog2.shared.utilities.StringUtils.f;

public class SupportBundleService {

    private final ExecutorService executor;
    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;

    @Inject
    public SupportBundleService(@Named("daemonScheduler") ScheduledExecutorService executor, NodeService nodeService, RemoteInterfaceProvider remoteInterfaceProvider) {
        this.executor = executor;
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
    }

    public void buildBundle(HttpHeaders httpHeaders, Subject currentSubject) {
        final ProxiedResourceHelper proxiedResourceHelper = new ProxiedResourceHelper(httpHeaders, currentSubject, nodeService, remoteInterfaceProvider, executor);

        final Map<String, ProxiedResource.CallResult<SupportBundleNodeManifest>> manifests = proxiedResourceHelper.requestOnAllNodes(proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSupportBundleInterface.class), RemoteSupportBundleInterface::getNodeManifest, Duration.ofSeconds(10));

        final Map<String, ProxiedResource.CallResult<ResponseBody>> stringCallResultMap = proxiedResourceHelper.requestOnAllNodes(proxiedResourceHelper.createRemoteInterfaceProvider(RemoteSupportBundleInterface.class), client -> client.getLogFile("0"), Duration.ofSeconds(10));
        try {
            //stringCallResultMap.values().stream().findFirst().get().response().entity().get().write(System.out);
            stringCallResultMap.values().stream().findFirst().get().response().entity().get().byteStream().transferTo(System.err);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SupportBundleNodeManifest getManifest() {

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();

        // TODO maybe try different RollingFileAppenders?
        final RollingFileAppender rollingFileAppender = (RollingFileAppender) config.getAppenders().get("rolling-file");
        if (rollingFileAppender != null) {
            final String filePattern = rollingFileAppender.getFilePattern();
            final String baseFileName = rollingFileAppender.getFileName();

            final ImmutableList.Builder<LogFile> logFiles = ImmutableList.builder();

            fromFileName("0", rollingFileAppender.getFileName()).ifPresent(logFiles::add);

            var regex = f("^%s\\.%%i\\.gz", baseFileName);
            if (filePattern.matches(regex)) {
                final String formatString = filePattern.replace("%i", "%d");
                IntStream.range(1, 5).forEach(i -> {
                    var file = f(formatString, i);
                    fromFileName(String.valueOf(i), file).ifPresent(logFiles::add);
                });
            }
            return new SupportBundleNodeManifest(new BundleEntries(logFiles.build()));
        }

        return new SupportBundleNodeManifest(new BundleEntries(List.of()));
    }


    private Optional<LogFile> fromFileName(String id, String fileName) {
        try {
            final Path filePath = Path.of(fileName);
            final long size = Files.size(filePath);
            final FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
            return Optional.of(new LogFile(id, fileName, size, lastModifiedTime.toInstant()));
            //return Optional.of(new LogFile(id, fileName, size, lastModifiedTime.toInstant().atZone(ZoneId.of("UTC"))));
        } catch (IOException e) {
            //TODO LOG?
            return Optional.empty();
        }

    }

    public void loadLogFileStream(LogFile logFile, OutputStream outputStream) throws IOException {
        Files.copy(Path.of(logFile.name()), outputStream);
    }


    class ProxiedResourceHelper extends ProxiedResource {
        private Subject currentSubject;

        protected ProxiedResourceHelper(HttpHeaders httpHeaders, Subject currentSubject, NodeService nodeService, RemoteInterfaceProvider remoteInterfaceProvider, ExecutorService executorService) {
            super(httpHeaders, nodeService, remoteInterfaceProvider, executorService);
            this.currentSubject = currentSubject;
        }

        @Override
        public String getAuthenticationToken() {
            return super.getAuthenticationToken();
        }

        @Override
        protected Subject getSubject() {
            return currentSubject;
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
