package org.graylog.testing;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ExecutableTestMain {
    static {
        // Set up JDK Logging adapter, https://logging.apache.org/log4j/2.x/log4j-jul/index.html
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        System.setProperty("graylog.executable-test-jar", "true");
    }

    private static final Logger LOG = LoggerFactory.getLogger(ExecutableTestMain.class);

    private static final List<DiscoverySelector> PACKAGE_SELECTORS = List.of(
            DiscoverySelectors.selectPackage("org.graylog"),
            DiscoverySelectors.selectPackage("org.graylog2")
    );

    public static void main(String[] args) {
        LOG.info("Starting tests: {}", Arrays.stream(args).toList());

        final var selectors = args.length > 0 ? List.of(DiscoverySelectors.selectClass(args[0])) : PACKAGE_SELECTORS;

        LOG.info("Selectors: {}", selectors);

        System.out.println("Test Runner started by: " + System.getProperty("user.name"));
        System.out.println("Current time: " + LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)));
        System.out.println("Running all tests...\n");

        final var launcher = LauncherFactory.create();
        final var listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.registerTestExecutionListeners(LoggingListener.forBiConsumer(
                (throwable, stringSupplier) -> LOG.info(stringSupplier.get(), throwable)
        ));

        final var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors)
//                .filters(new ClassNameFilter() {
//                    @Override
//                    public FilterResult apply(String className) {
//                        System.out.println(className + ": " + className.endsWith("IT"));
//                        return FilterResult.includedIf(className.endsWith("IT"));
//                    }
//                })
                // TODO: For some reason we don't execute any test when we set a filter. Need to be investigated.
                .filters(ClassNameFilter.includeClassNamePatterns("^.+IT$"))
                .build();

        launcher.execute(request);

        final var summary = listener.getSummary();

        System.out.println("\nTest Results:");
        System.out.println("Tests Found: " + summary.getTestsFoundCount());
        System.out.println("Tests Started: " + summary.getTestsStartedCount());
        System.out.println("Tests Succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests Failed: " + summary.getTestsFailedCount());
        System.out.println("Tests Skipped: " + summary.getTestsSkippedCount());
        System.out.println("Time: " + summary.getTimeFinished() + "ms");

        final var failures = summary.getFailures();
        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            PrintWriter writer = new PrintWriter(System.out, false, StandardCharsets.UTF_8);
            failures.forEach(failure -> {
                System.out.println(failure.getTestIdentifier().getDisplayName() + " - " + failure.getException().getMessage());
                failure.getException().printStackTrace(writer);
                writer.flush();
            });
            System.exit(1);
        } else {
            System.out.println("\nAll tests passed!");
        }
    }
}
