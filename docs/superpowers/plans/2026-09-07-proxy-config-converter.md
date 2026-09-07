# ProxyConfigConverter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `http_proxy_uri` bind as `ProxyConfig` (nullable) everywhere it's injected via `@Named("http_proxy_uri")`, in both `graylog2-server` and `graylog-plugin-enterprise`, replacing every consumer's own `URI`-parsing with `ProxyConfig` accessors — and restore proxy support to `ChatModelFactory`'s AI agent runtime HTTP clients, built directly on the new pattern.

**Architecture:** A new JadConfig `Converter<ProxyConfig>` (`ProxyConfigConverter`, mirroring the existing `ProxyHostsPatternConverter`) parses `http_proxy_uri`'s raw string directly into a `ProxyConfig` at config-load time. `BaseConfiguration`'s `@Parameter` field changes from `URI` to `ProxyConfig`, so `@Named("http_proxy_uri")` is now Guice-bound as `@Nullable ProxyConfig` everywhere, not `@Nullable URI`. Every current consumer of the raw `URI` (seven classes across two repos) is migrated to consume `ProxyConfig` instead, deleting whatever host/port/credential/endpoint parsing they used to hand-roll.

**Tech Stack:** Java 21, JadConfig (`Converter`), Guice, Guava (`Splitter`), JUnit 5, AssertJ, Maven, langchain4j 1.19.0, AWS SDK v2 (`apache-client`, `apache5-client`, `netty-nio-client`).

**Spec:** `docs/superpowers/specs/2026-09-07-proxy-config-converter-design.md` (this same graylog2-server repo, committed on `proxy-config-record`).

## Global Constraints

- No change to `graylog.conf` format — `http_proxy_uri`'s raw string value and semantics are unchanged; only how it's parsed and typed internally changes.
- `ProxyConfig` (graylog2-server, `org.graylog2.utilities`) still builds no client-specific proxy object of its own (no `java.net.ProxySelector`, no OkHttp/JDK `Authenticator`, no AWS `ProxyConfiguration`) — every migrated consumer keeps building its own client-specific object, just from `ProxyConfig` accessors instead of raw `URI` parsing.
- `ProxyConfig` does not hold `ProxyHostsPattern` (`http_non_proxy_hosts`) — unchanged, stays a separate constructor parameter wherever both are needed (`ProxySelectorProvider`).
- `ProxySelectorProvider.getProxyAddress(): InetSocketAddress` must remain `public`, and `select()` must keep calling `this.getProxyAddress()` rather than reading proxy host/port inline — `OkHttpClientProviderTest#testDynamicProxy` `Mockito.spy()`s a `ProxySelectorProvider` and stubs `getProxyAddress()` mid-test to prove `select()` re-reads it on every call.
- Six of the seven touched consumers (`ProxySelectorProvider`, `OkHttpClientProvider`, `AWSProxyConfigurationProvider`, `AWSAsyncProxyConfigurationProvider`, `GitRepositoryService`, `AWSInstanceNameLookupProcessor`) are pure refactors: no behavior change, existing tests re-verify the same assertions against updated call sites. Only `ChatModelFactory` (Task 11) is new functionality.
- This sandbox CAN run real Maven test suites offline (`./mvnw -o test ...`), run from the meta-project root (`graylog-project-internal`, sibling-layout workspace already set up at `.worktrees/proxy-config-record/graylog-project-internal`) — do not use a javac/JUnit-Platform-Launcher fallback unless a real `-o` Maven run fails for a reason unrelated to the code under test.
- Task 11 (`ChatModelFactory`) requires graylog2-server's changes `mvn install`ed locally first, so `graylog-plugin-enterprise` compiles against the updated jar rather than a stale one.

---

### Task 1: Merge the `OkHttpClientProviderTest` flakiness fix

**Files:**
- Modify (via cherry-pick, no manual edits): `graylog2-server/graylog2-server/src/test/java/org/graylog2/shared/bindings/providers/OkHttpClientProviderTest.java`

**Interfaces:** None — this task touches only test fixture content, not production code. No later task depends on anything new here beyond "the test class is enabled and uses a numeric-IP literal target instead of `www.example.com`."

This test class is currently `@Disabled` (class-level, plus two individually-`@Disabled` methods) on `proxy-config-record` — unrelated to this plan, it's the same pre-existing state as on `master`. A fix already exists on a separate branch, `fix-okhttpclientprovider-test-flakiness` (based on `master` at the same commit `proxy-config-record` branched from), as two commits: `6a9f2c4e3e` ("Re-enable OkHttpClientProviderTest, remove its live-DNS dependency") and `ed56c0bdb8` ("Fix imprecise Javadoc link in OkHttpClientProviderTest comment"). Both commits touch only this one test file, which `proxy-config-record`'s Tasks 1-4 (already committed) never touched — confirmed byte-identical to their common base — so this cherry-pick applies cleanly with no conflicts.

Merging this now (before Tasks 5-6 touch this same file's `client()` helper) avoids maintaining two diverging copies of the same test.

- [ ] **Step 1: Cherry-pick both commits**

From the `graylog2-server` repo root (`.worktrees/proxy-config-record/graylog-project-repos/graylog2-server`):

```bash
git cherry-pick 6a9f2c4e3ee3ec500221415ad604b950b2dd5e8a ed56c0bdb80fa6ac58fba05a0ce18392945e33eb
```

Expected: both apply cleanly, no conflicts (confirmed identical base file). If a conflict does occur, STOP and report it rather than resolving blindly — it would mean something else has touched this file unexpectedly.

- [ ] **Step 2: Run the test suite to verify it's enabled and passing**

From the meta-project root (`.worktrees/proxy-config-record/graylog-project-internal`):

```bash
./mvnw -o test -pl :graylog2-server -Dtest=OkHttpClientProviderTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0` (previously it was 14 skipped; the cherry-picked fix both re-enables the class and de-flakes its DNS dependency in one change).

No commit step — the cherry-picks already created their own commits.

---

### Task 2: `ProxyConfig.port(int httpDefault, int httpsDefault)`

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfig.java`
- Modify: `graylog2-server/graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigTest.java`

**Interfaces:**
- Produces: `ProxyConfig.port(int httpDefault, int httpsDefault): int` — returns `port()` when non-negative, otherwise `httpsDefault` if `scheme()` equals `"https"` (case-insensitive), otherwise `httpDefault`. Task 8 (`AWSAsyncProxyConfigurationProvider`) calls this exact signature.

`ProxyConfig.java`'s current content (full file, for reference — only the addition below changes):

```java
package org.graylog2.utilities;

import com.google.common.base.Splitter;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.utilities.StringUtils.f;

public record ProxyConfig(URI uri) {

    public record Credentials(String username, String password) {}

    public static Optional<ProxyConfig> from(@Nullable URI uri) {
        return Optional.ofNullable(uri).map(ProxyConfig::new);
    }

    public String host() {
        return uri.getHost();
    }

    public int port() {
        return uri.getPort();
    }

    public String scheme() {
        return uri.getScheme();
    }

    public URI endpoint() {
        return URI.create(f("%s://%s:%d", scheme(), host(), port()));
    }

    public Optional<Credentials> credentials() {
        if (isNullOrEmpty(uri.getUserInfo())) {
            return Optional.empty();
        }
        final List<String> userInfo = Splitter.on(':').limit(2).splitToList(uri.getUserInfo());
        return userInfo.size() == 2
                ? Optional.of(new Credentials(userInfo.get(0), userInfo.get(1)))
                : Optional.empty();
    }
}
```

(`from(URI)` stays for now — it's removed in Task 6, once its last production caller is migrated. Do not remove it in this task.)

- [ ] **Step 1: Write the failing tests**

Add these three test methods to `ProxyConfigTest.java` (inside the existing class, alongside the current tests — do not remove or modify any existing test in this task):

```java
    @Test
    public void portWithDefaultsReturnsTheExplicitPortWhenPresent() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://proxy.example.com:8123"));

        assertThat(config.port(80, 443)).isEqualTo(8123);
    }

    @Test
    public void portWithDefaultsFallsBackToTheHttpDefaultForHttpWithNoPort() {
        final ProxyConfig config = new ProxyConfig(URI.create("http://proxy.example.com"));

        assertThat(config.port(80, 443)).isEqualTo(80);
    }

    @Test
    public void portWithDefaultsFallsBackToTheHttpsDefaultForHttpsWithNoPort() {
        final ProxyConfig config = new ProxyConfig(URI.create("https://proxy.example.com"));

        assertThat(config.port(80, 443)).isEqualTo(443);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

From the meta-project root:

```bash
./mvnw -o test -pl :graylog2-server -Dtest=ProxyConfigTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: compile failure — `port(int, int)` does not exist yet (`cannot find symbol`).

- [ ] **Step 3: Add the method**

In `ProxyConfig.java`, add this method immediately after `port()`:

```java
    public int port(int httpDefault, int httpsDefault) {
        final int port = port();
        return port >= 0 ? port : ("https".equalsIgnoreCase(scheme()) ? httpsDefault : httpDefault);
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=ProxyConfigTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 13, Failures: 0, Errors: 0` (10 existing + 3 new).

- [ ] **Step 5: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfig.java \
        graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigTest.java
git commit -m "Add ProxyConfig.port(int, int) for scheme-aware default-port fallback"
```

---

### Task 3: `ProxyConfigConverter`

**Files:**
- Create: `graylog2-server/graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfigConverter.java`
- Create: `graylog2-server/graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigConverterTest.java`

**Interfaces:**
- Consumes: `ProxyConfig` (record with `URI uri` constructor — Task 1, already committed).
- Produces: `org.graylog2.utilities.ProxyConfigConverter implements com.github.joschi.jadconfig.Converter<ProxyConfig>` with `public ProxyConfig convertFrom(String value)` and `public String convertTo(ProxyConfig value)`. Task 4 references this class by name in a `@Parameter(converter = ProxyConfigConverter.class)` annotation.

Mirrors the existing `ProxyHostsPatternConverter.java` (same package) exactly in shape:

```java
package org.graylog2.utilities;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;

public class ProxyHostsPatternConverter implements Converter<ProxyHostsPattern> {
    @Override
    public ProxyHostsPattern convertFrom(String value) {
        try {
            return ProxyHostsPattern.create(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Invalid proxy hosts pattern: \"" + value + "\"", e);
        }
    }

    @Override
    public String convertTo(ProxyHostsPattern value) {
        return value.getNoProxyHosts();
    }
}
```

JadConfig only invokes `convertFrom` for a non-empty configured value (confirmed from this existing converter's own contract) — when `http_proxy_uri` is unset, the field stays `null` and the converter never runs, so `convertFrom` never needs to handle a null/empty `value`.

- [ ] **Step 1: Write the failing tests**

Create `graylog2-server/graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigConverterTest.java`:

```java
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
package org.graylog2.utilities;

import com.github.joschi.jadconfig.ParameterException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProxyConfigConverterTest {

    @Test
    public void convertFromParsesAConfiguredProxy() {
        final ProxyConfigConverter converter = new ProxyConfigConverter();

        final ProxyConfig config = converter.convertFrom("http://user:pass@proxy.example.com:8123");

        assertThat(config.uri()).isEqualTo(URI.create("http://user:pass@proxy.example.com:8123"));
        assertThat(config.host()).isEqualTo("proxy.example.com");
        assertThat(config.port()).isEqualTo(8123);
    }

    @Test
    public void convertFromRejectsAMalformedUri() {
        final ProxyConfigConverter converter = new ProxyConfigConverter();

        assertThatThrownBy(() -> converter.convertFrom("://not a uri"))
                .isInstanceOf(ParameterException.class);
    }

    @Test
    public void convertToRoundTripsToTheOriginalUriString() {
        final ProxyConfigConverter converter = new ProxyConfigConverter();
        final ProxyConfig config = new ProxyConfig(URI.create("http://proxy.example.com:8123"));

        assertThat(converter.convertTo(config)).isEqualTo("http://proxy.example.com:8123");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=ProxyConfigConverterTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: compile failure — `ProxyConfigConverter` does not exist yet.

- [ ] **Step 3: Write the minimal implementation**

Create `graylog2-server/graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfigConverter.java`:

```java
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
package org.graylog2.utilities;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;

import java.net.URI;

public class ProxyConfigConverter implements Converter<ProxyConfig> {
    @Override
    public ProxyConfig convertFrom(String value) {
        try {
            return new ProxyConfig(URI.create(value));
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Invalid proxy URI: \"" + value + "\"", e);
        }
    }

    @Override
    public String convertTo(ProxyConfig value) {
        return value.uri().toString();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=ProxyConfigConverterTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfigConverter.java \
        graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigConverterTest.java
git commit -m "Add ProxyConfigConverter, parsing http_proxy_uri directly into a ProxyConfig"
```

---

### Task 4: `BaseConfiguration` — bind `http_proxy_uri` as `ProxyConfig`

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog2/plugin/BaseConfiguration.java`

**Interfaces:**
- Consumes: `ProxyConfigConverter` (Task 3).
- Produces: `@Named("http_proxy_uri")` now Guice-binds as `@Nullable ProxyConfig` (previously `@Nullable URI`). `BaseConfiguration.getHttpProxyUri()` is **removed**. `getHttpProxyConfig(): Optional<ProxyConfig>` keeps its existing signature (Task 2 of the original plan, already committed) but its implementation simplifies.

**This is the pivot task.** After this commit, every class still injecting `@Named("http_proxy_uri") @Nullable URI` (Tasks 5-10 below) has nothing bound to that key — Guice will fail at injector-creation time for any of them (a runtime wiring error, not a compile error: the Java source of an un-migrated consumer still compiles fine, since Guice bindings aren't compile-checked). None of this plan's own unit tests go through full Guice injector bootstrapping — they construct classes directly — so each task's own tests stay green throughout; but the branch should not be treated as safe to boot as a full server until Tasks 5-10 are all done.

Current `BaseConfiguration.java` imports (lines 19-44):

```java
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.CommonNodeConfiguration;
import org.graylog2.configuration.PathConfiguration;
import org.graylog2.shared.messageq.MessageQueueModule;
import org.graylog2.utilities.ProxyConfig;
import org.graylog2.utilities.ProxyHostsPattern;
import org.graylog2.utilities.ProxyHostsPatternConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
```

`java.net.URI` is used nowhere else in this file (confirmed by search) — it becomes unused once `httpProxyUri`/`getHttpProxyUri()` are removed below.

- [ ] **Step 1: Update the import block**

Remove the `java.net.URI` import; add `ProxyConfigConverter` (alphabetically after `ProxyConfig`, before `ProxyHostsPattern`):

```java
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.CommonNodeConfiguration;
import org.graylog2.configuration.PathConfiguration;
import org.graylog2.shared.messageq.MessageQueueModule;
import org.graylog2.utilities.ProxyConfig;
import org.graylog2.utilities.ProxyConfigConverter;
import org.graylog2.utilities.ProxyHostsPattern;
import org.graylog2.utilities.ProxyHostsPatternConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
```

- [ ] **Step 2: Change the field declaration**

Current (lines 134-143):

```java
    @Documentation("""
            HTTP proxy for outgoing HTTP connections
            ATTENTION: If you configure a proxy, make sure to also configure the "http_non_proxy_hosts" option so internal
                       HTTP connections with other nodes does not go through the proxy.
            Examples:
              - http://proxy.example.com:8123
              - http://username:password@proxy.example.com:8123
            """)
    @Parameter(value = "http_proxy_uri")
    private URI httpProxyUri;
```

Change to:

```java
    @Documentation("""
            HTTP proxy for outgoing HTTP connections
            ATTENTION: If you configure a proxy, make sure to also configure the "http_non_proxy_hosts" option so internal
                       HTTP connections with other nodes does not go through the proxy.
            Examples:
              - http://proxy.example.com:8123
              - http://username:password@proxy.example.com:8123
            """)
    @Parameter(value = "http_proxy_uri", converter = ProxyConfigConverter.class)
    private ProxyConfig httpProxyConfig;
```

- [ ] **Step 3: Replace the getters**

Current (lines 273-279):

```java
    public URI getHttpProxyUri() {
        return httpProxyUri;
    }

    public Optional<ProxyConfig> getHttpProxyConfig() {
        return ProxyConfig.from(httpProxyUri);
    }
```

Change to:

```java
    public Optional<ProxyConfig> getHttpProxyConfig() {
        return Optional.ofNullable(httpProxyConfig);
    }
```

- [ ] **Step 4: Compile to verify**

From the meta-project root:

```bash
./mvnw -o compile -pl :graylog2-server -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: **compile FAILURE** — `ProxySelectorProvider`, `OkHttpClientProvider`, `AWSProxyConfigurationProvider`, `AWSAsyncProxyConfigurationProvider` still reference `httpProxyUri`/expect a `URI` in ways that no longer type-check once downstream tasks are done... actually check carefully: those classes inject their OWN `@Named("http_proxy_uri") @Nullable URI` parameter — that's their own local variable name, entirely independent of `BaseConfiguration`'s field name, and Guice bindings aren't compile-checked. So this compile is expected to **succeed** — Java source for those other classes doesn't reference `BaseConfiguration.httpProxyUri` or `getHttpProxyUri()` directly (confirmed: `getHttpProxyUri()`'s only caller found in the whole codebase was `AWSInstanceNameLookupProcessor`, migrated in Task 10). If compilation fails here for a different, unexpected reason, investigate before proceeding — it would mean an undiscovered caller of the removed `getHttpProxyUri()` exists.

- [ ] **Step 5: Search for any remaining callers of the removed getter, to be sure**

```bash
grep -rn "getHttpProxyUri" graylog2-server/src/main/java/
```

Expected: exactly one hit, in `AWSInstanceNameLookupProcessor.java` (handled in Task 10 — leave it for now, its own task will fix that file). If there are hits anywhere else, STOP and report — this plan's scope would need to expand to cover that additional caller.

- [ ] **Step 6: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog2/plugin/BaseConfiguration.java
git commit -m "Bind http_proxy_uri as ProxyConfig via ProxyConfigConverter"
```

---

### Task 5: Revise `ProxySelectorProvider` to inject `ProxyConfig` directly

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog2/shared/bindings/providers/ProxySelectorProvider.java`
- Test (unchanged, re-run only): `graylog2-server/graylog2-server/src/test/java/org/graylog2/shared/bindings/providers/OkHttpClientProviderTest.java`

**Interfaces:**
- Consumes: `ProxyConfig` (record — Task 1).
- Produces: `ProxySelectorProvider`'s public API is **unchanged** — same constructor shape `(ProxyConfig, ProxyHostsPattern)` (only the first parameter's declared type changes, from `URI` to `ProxyConfig`), same `get(): ProxySelector`, same `getProxyAddress(): InetSocketAddress`.

Current full file:

```java
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
package org.graylog2.shared.bindings.providers;

import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.graylog2.utilities.ProxyConfig;
import org.graylog2.utilities.ProxyHostsPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

public class ProxySelectorProvider implements Provider<ProxySelector> {
    private static final Logger LOG = LoggerFactory.getLogger(ProxySelectorProvider.class);

    protected final Optional<ProxyConfig> proxyConfig;
    protected final ProxyHostsPattern nonProxyHostsPattern;

    @Inject
    public ProxySelectorProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri,
                                 @Named("http_non_proxy_hosts") @Nullable ProxyHostsPattern nonProxyHostsPattern) {
        this.proxyConfig = ProxyConfig.from(httpProxyUri);
        this.nonProxyHostsPattern = nonProxyHostsPattern;
    }

    @Override
    public ProxySelector get() {
        if (proxyConfig.isEmpty()) {
            return ProxySelector.getDefault();
        }
        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                final String host = uri.getHost();
                if (nonProxyHostsPattern != null && nonProxyHostsPattern.matches(host)) {
                    LOG.debug("Bypassing proxy server for {}", host);
                    return ImmutableList.of(Proxy.NO_PROXY);
                }
                try {
                    final InetAddress targetAddress = InetAddress.getByName(host);
                    if (targetAddress.isLoopbackAddress()) {
                        return ImmutableList.of(Proxy.NO_PROXY);
                    } else if (nonProxyHostsPattern != null && nonProxyHostsPattern.matches(targetAddress.getHostAddress())) {
                        LOG.debug("Bypassing proxy server for {}", targetAddress.getHostAddress());
                        return ImmutableList.of(Proxy.NO_PROXY);
                    }
                } catch (UnknownHostException e) {
                    LOG.debug("Unable to resolve host name for proxy selection: ", e);
                }

                final Proxy proxy = new Proxy(Proxy.Type.HTTP, getProxyAddress());
                return ImmutableList.of(proxy);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                LOG.warn("Unable to connect to proxy: ", ioe);
            }
        };
    }

    public InetSocketAddress getProxyAddress() {
        final ProxyConfig config = proxyConfig.orElseThrow();
        return new InetSocketAddress(config.host(), config.port());
    }
}
```

Only the constructor changes — `select()`, `get()`, `getProxyAddress()`, and every import except the ones the constructor itself needs are untouched.

- [ ] **Step 1: Change the constructor**

Current:

```java
    @Inject
    public ProxySelectorProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri,
                                 @Named("http_non_proxy_hosts") @Nullable ProxyHostsPattern nonProxyHostsPattern) {
        this.proxyConfig = ProxyConfig.from(httpProxyUri);
        this.nonProxyHostsPattern = nonProxyHostsPattern;
    }
```

Change to:

```java
    @Inject
    public ProxySelectorProvider(@Named("http_proxy_uri") @Nullable ProxyConfig httpProxyConfig,
                                 @Named("http_non_proxy_hosts") @Nullable ProxyHostsPattern nonProxyHostsPattern) {
        this.proxyConfig = Optional.ofNullable(httpProxyConfig);
        this.nonProxyHostsPattern = nonProxyHostsPattern;
    }
```

The `java.net.URI` import stays — it's still used by `select(URI uri)`'s parameter type and `connectFailed(URI uri, ...)`.

- [ ] **Step 2: Update `OkHttpClientProviderTest.java`'s construction call sites**

This test file constructs `ProxySelectorProvider` directly in three places. Update each — `null` literals need no change (assignable to either type); real `URI.create(...)`/`server.url("/").uri()` values need wrapping in `new ProxyConfig(...)`.

In the `client(URI proxyURI)` helper (near the end of the file):

Current:

```java
    private OkHttpClient client(URI proxyURI) {
        final OkHttpClientProvider provider = new OkHttpClientProvider(
                "GraylogTest",
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                proxyURI, null, new ProxySelectorProvider(proxyURI, null));

        return provider.get();
    }
```

Change to:

```java
    private OkHttpClient client(URI proxyURI) {
        final ProxyConfig proxyConfig = proxyURI == null ? null : new ProxyConfig(proxyURI);
        final OkHttpClientProvider provider = new OkHttpClientProvider(
                "GraylogTest",
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                proxyConfig, null, new ProxySelectorProvider(proxyConfig, null));

        return provider.get();
    }
```

(`proxyURI` itself stays a `URI` parameter here — only what gets passed into the two providers changes. Task 6 needs this same `proxyConfig` local variable, which is why it's introduced once here rather than inline twice.)

In `testDynamicProxy()`:

Current:

```java
        final ProxySelectorProvider proxyProvider = new ProxySelectorProvider(server.url("/").uri(), null);
        ProxySelectorProvider spyProxyProvider = Mockito.spy(proxyProvider);
        final OkHttpClientProvider provider = new OkHttpClientProvider(
                "GraylogTest",
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                server.url("/").uri(), null, spyProxyProvider);
```

Change to:

```java
        final ProxyConfig testProxyConfig = new ProxyConfig(server.url("/").uri());
        final ProxySelectorProvider proxyProvider = new ProxySelectorProvider(testProxyConfig, null);
        ProxySelectorProvider spyProxyProvider = Mockito.spy(proxyProvider);
        final OkHttpClientProvider provider = new OkHttpClientProvider(
                "GraylogTest",
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                testProxyConfig, null, spyProxyProvider);
```

Current import block (top of file, after Task 1's flakiness-fix commits):

```java
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.net.HttpHeaders;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
```

Change to (`org.graylog2.utilities.ProxyConfig` inserted before `org.junit.jupiter.api.AfterEach` — "graylog2" sorts before "junit"):

```java
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.net.HttpHeaders;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.graylog2.utilities.ProxyConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
```

- [ ] **Step 3: Run the test suite to verify no regression**

From the meta-project root:

```bash
./mvnw -o test -pl :graylog2-server -Dtest=OkHttpClientProviderTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0` — same as Task 1's baseline, in particular `testDynamicProxy` (exercises `getProxyAddress()` being called dynamically via the spy) and every `testFailingProxyConnectionWithAuthentication*`/`testSuccessfulProxyConnectionWithAuthentication*` test.

Note: this step will currently fail to compile until Task 6 also updates `OkHttpClientProvider`'s constructor (same file, same test class, both providers' signatures change together) — if so, proceed to Task 6 before re-running this verification; Task 6's Step 3 re-runs this exact command as its own regression check, which is the real gate for both tasks together.

- [ ] **Step 4: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog2/shared/bindings/providers/ProxySelectorProvider.java \
        graylog2-server/src/test/java/org/graylog2/shared/bindings/providers/OkHttpClientProviderTest.java
git commit -m "Inject ProxyConfig directly into ProxySelectorProvider"
```

---

### Task 6: Revise `OkHttpClientProvider` to inject `ProxyConfig` directly; remove `ProxyConfig.from(URI)`

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog2/shared/bindings/providers/OkHttpClientProvider.java`
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfig.java`
- Modify: `graylog2-server/graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigTest.java`
- Test (already updated in Task 5, re-run only): `graylog2-server/graylog2-server/src/test/java/org/graylog2/shared/bindings/providers/OkHttpClientProviderTest.java`

**Interfaces:**
- Consumes: `ProxyConfig` (Task 1); `ProxySelectorProvider` (Task 5) — used exactly as before via `proxySelectorProvider.get()`.
- Produces: `OkHttpClientProvider`'s public API is unchanged apart from the proxy parameter's declared type (`URI` → `ProxyConfig`). `ProxyConfig.from(URI)` no longer exists after this task.

Current `OkHttpClientProvider.java` (relevant portions):

```java
    protected final String userAgent;
    protected final Duration connectTimeout;
    protected final Duration readTimeout;
    protected final Duration writeTimeout;
    protected final Optional<ProxyConfig> proxyConfig;
    private final TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider;
    private final ProxySelectorProvider proxySelectorProvider;

    @Inject
    public OkHttpClientProvider(@Named("http_user_agent") String userAgent,
                                @Named("http_connect_timeout") Duration connectTimeout,
                                @Named("http_read_timeout") Duration readTimeout,
                                @Named("http_write_timeout") Duration writeTimeout,
                                @Named("http_proxy_uri") @Nullable URI httpProxyUri,
                                TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider,
                                ProxySelectorProvider proxySelectorProvider) {
        this.userAgent = requireNonNull(userAgent);
        this.connectTimeout = requireNonNull(connectTimeout);
        this.readTimeout = requireNonNull(readTimeout);
        this.writeTimeout = requireNonNull(writeTimeout);
        this.proxyConfig = ProxyConfig.from(httpProxyUri);
        this.trustManagerAndSocketFactoryProvider = trustManagerAndSocketFactoryProvider;
        this.proxySelectorProvider = proxySelectorProvider;
    }
```

- [ ] **Step 1: Change the constructor**

Change the parameter and its assignment:

```java
    @Inject
    public OkHttpClientProvider(@Named("http_user_agent") String userAgent,
                                @Named("http_connect_timeout") Duration connectTimeout,
                                @Named("http_read_timeout") Duration readTimeout,
                                @Named("http_write_timeout") Duration writeTimeout,
                                @Named("http_proxy_uri") @Nullable ProxyConfig httpProxyConfig,
                                TrustManagerAndSocketFactoryProvider trustManagerAndSocketFactoryProvider,
                                ProxySelectorProvider proxySelectorProvider) {
        this.userAgent = requireNonNull(userAgent);
        this.connectTimeout = requireNonNull(connectTimeout);
        this.readTimeout = requireNonNull(readTimeout);
        this.writeTimeout = requireNonNull(writeTimeout);
        this.proxyConfig = Optional.ofNullable(httpProxyConfig);
        this.trustManagerAndSocketFactoryProvider = trustManagerAndSocketFactoryProvider;
        this.proxySelectorProvider = proxySelectorProvider;
    }
```

Check the import block: `java.net.URI` was already removed from this file in the original Task 4 (of the earlier, already-committed plan) — confirm it's not reintroduced; no import changes should be needed here since `ProxyConfig` is already imported and `URI` is no longer referenced anywhere in this file. `get()` is untouched (it already operates on `proxyConfig`, not a raw URI).

- [ ] **Step 2: Update `OkHttpClientProviderTest.java`'s remaining call sites**

Task 5 already updated the `client()` helper and `testDynamicProxy()` to construct `ProxyConfig` and pass it to both providers. Verify (no further edit needed if Task 5 was done first): both `new OkHttpClientProvider(...)` call sites in that file now pass a `ProxyConfig`/`null` (not a `URI`) as the fifth constructor argument. If Tasks 5 and 6 are being done in one continuous session, this step is already satisfied by Task 5 Step 2 — just confirm by re-reading the file.

- [ ] **Step 3: Run the test suite to verify no regression**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=OkHttpClientProviderTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0` — this is the real regression gate for both Task 5 and Task 6 together, in particular the four `testFailingProxyConnectionWithAuthentication*` cases (`user:password@`, `:password@`, `user:@`, `:@`), which are exactly the shapes `ProxyConfig.credentials()` must reproduce.

- [ ] **Step 4: Confirm `ProxyConfig.from(URI)` has no remaining callers**

```bash
grep -rn "ProxyConfig.from\|ProxyConfig::from" graylog2-server/src/main/java/
```

Expected: no hits (Tasks 4, 5, and this task's Step 1 removed the three production call sites: `BaseConfiguration.getHttpProxyConfig()`, `ProxySelectorProvider`'s constructor, `OkHttpClientProvider`'s constructor). If any hit remains, STOP — do not proceed to Step 5 until it's accounted for.

- [ ] **Step 5: Remove `ProxyConfig.from(URI)` and its dedicated tests**

In `ProxyConfig.java`, remove:

```java
    public static Optional<ProxyConfig> from(@Nullable URI uri) {
        return Optional.ofNullable(uri).map(ProxyConfig::new);
    }

```

(including the blank line after it). Remove the now-unused `javax.annotation.Nullable` import (confirm nothing else in the file uses `@Nullable` — it doesn't, this was the only usage).

In `ProxyConfigTest.java`, remove these two test methods:

```java
    @Test
    public void fromReturnsEmptyWithoutAConfiguredProxy() {
        assertThat(ProxyConfig.from(null)).isEmpty();
    }

    @Test
    public void fromWrapsAConfiguredProxy() {
        assertThat(ProxyConfig.from(URI.create("http://proxy.example.com:8123"))).isPresent();
    }

```

- [ ] **Step 6: Run the full `ProxyConfig`/`ProxyConfigConverter` test suite to verify no regression**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=ProxyConfigTest,ProxyConfigConverterTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 14, Failures: 0, Errors: 0` (13 from Task 2 minus 2 removed `.from()` tests, plus 3 from `ProxyConfigConverterTest` = 14).

- [ ] **Step 7: Full module compile check**

```bash
./mvnw -o test-compile -pl :graylog2-server -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `BUILD SUCCESS`. This is the first point where every remaining `@Named("http_proxy_uri") @Nullable URI` consumer (`AWSProxyConfigurationProvider`, `AWSAsyncProxyConfigurationProvider`) still compiles fine as Java source (their own local parameter is still declared `URI` — that's legal Java regardless of what Guice has bound) but would fail at Guice injector-creation time if the server were actually booted. Tasks 7-8 fix that.

- [ ] **Step 8: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog2/shared/bindings/providers/OkHttpClientProvider.java \
        graylog2-server/src/main/java/org/graylog2/utilities/ProxyConfig.java \
        graylog2-server/src/test/java/org/graylog2/utilities/ProxyConfigTest.java \
        graylog2-server/src/test/java/org/graylog2/shared/bindings/providers/OkHttpClientProviderTest.java
git commit -m "Inject ProxyConfig directly into OkHttpClientProvider; remove now-dead ProxyConfig.from(URI)"
```

---

### Task 7: Migrate `AWSProxyConfigurationProvider`

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog/aws/AWSProxyConfigurationProvider.java`
- Modify: `graylog2-server/graylog2-server/src/test/java/org/graylog/aws/AWSProxyConfigurationProviderTest.java`

**Interfaces:**
- Consumes: `ProxyConfig.endpoint(): URI`, `ProxyConfig.credentials(): Optional<ProxyConfig.Credentials>` (Task 1).
- Produces: `AWSProxyConfigurationProvider`'s public API unchanged apart from the constructor parameter type; `buildProxyConfiguration` changes signature from `static ProxyConfiguration buildProxyConfiguration(URI proxyUri)` to `static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig)` — package-visible, called directly by `AWSProxyConfigurationProviderTest`.

Current full file:

```java
package org.graylog.aws;

import com.google.common.base.Splitter;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Singleton
public class AWSProxyConfigurationProvider implements Provider<ApacheHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSProxyConfigurationProvider.class);
    private final URI httpProxyUri;

    @Inject
    public AWSProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public ApacheHttpClient.Builder get() {
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (httpProxyUri == null) {
            LOG.debug("AWS proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(httpProxyUri));
        LOG.debug("AWS proxy enabled: {}:{}", httpProxyUri.getHost(), httpProxyUri.getPort());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(URI proxyUri) {
        ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder();

        if (proxyUri.getUserInfo() != null && !proxyUri.getUserInfo().isEmpty()) {
            final List<String> credentials = Splitter.on(":")
                    .limit(2)
                    .splitToList(proxyUri.getUserInfo());

            if (credentials.size() == 2) {
                proxyConfigBuilder.username(credentials.get(0));
                proxyConfigBuilder.password(credentials.get(1));
            }

            try {
                URI cleanProxyUri = new URI(
                        proxyUri.getScheme(),
                        null,
                        proxyUri.getHost(),
                        proxyUri.getPort(),
                        proxyUri.getPath(),
                        proxyUri.getQuery(),
                        proxyUri.getFragment()
                );
                proxyConfigBuilder.endpoint(cleanProxyUri);
            } catch (URISyntaxException e) {
                proxyConfigBuilder.endpoint(proxyUri);
            }
        } else {
            proxyConfigBuilder.endpoint(proxyUri);
        }

        return proxyConfigBuilder.build();
    }
}
```

Note the license header is absent in the current file (this class predates the SSPL header requirement in some parts of the codebase) — preserve the file exactly as-is otherwise, do not add a header as part of this unrelated change.

- [ ] **Step 1: Replace the whole file**

```java
package org.graylog.aws;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.utilities.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

@Singleton
public class AWSProxyConfigurationProvider implements Provider<ApacheHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSProxyConfigurationProvider.class);
    private final ProxyConfig proxyConfig;

    @Inject
    public AWSProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public ApacheHttpClient.Builder get() {
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (proxyConfig == null) {
            LOG.debug("AWS proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(proxyConfig));
        LOG.debug("AWS proxy enabled: {}:{}", proxyConfig.host(), proxyConfig.port());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig) {
        final ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder()
                .endpoint(proxyConfig.endpoint());
        proxyConfig.credentials().ifPresent(credentials ->
                proxyConfigBuilder.username(credentials.username()).password(credentials.password()));
        return proxyConfigBuilder.build();
    }
}
```

Behavior note (documented in the spec, not a regression to fix): the old code preserved the URI's path/query/fragment when reconstructing the userinfo-stripped endpoint; `ProxyConfig.endpoint()` does not. `http_proxy_uri` is documented and always used as a bare `scheme://[user:pass@]host:port` — this is an accepted simplification, not a behavior change in practice.

- [ ] **Step 2: Update the test file's call sites**

`AWSProxyConfigurationProviderTest.java` currently constructs a `URI` and calls the static method directly, 6 times. Update each `AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri)` call by wrapping its argument: change

```java
        URI proxyUri = URI.create("http://proxy.example.com:8080");

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);
```

(and the four other analogous blocks, each with a different URI literal) to:

```java
        ProxyConfig proxyUri = new ProxyConfig(URI.create("http://proxy.example.com:8080"));

        ProxyConfiguration proxyConfig = AWSProxyConfigurationProvider.buildProxyConfiguration(proxyUri);
```

Apply this same `URI.create(...)` → `new ProxyConfig(URI.create(...))` wrapping to all six test methods (`buildProxyConfigurationWithoutCredentials`, `buildProxyConfigurationWithCredentials`, `buildProxyConfigurationWithUsernameOnly`, `buildProxyConfigurationWithHttpsScheme`, `buildProxyConfigurationWithEmptyCredentials`, `buildProxyConfigurationWithDefaultPort`) — each keeps its local variable named `proxyUri` for minimal diff, just re-typed. Add `import org.graylog2.utilities.ProxyConfig;` to the test file's imports (alphabetically, after `org.junit.jupiter.api.Test`, before `software.amazon.awssdk...`).

Every assertion in the test file stays exactly as-is — the returned `ProxyConfiguration`'s `host()`/`port()`/`username()`/`password()` values are unchanged (`ProxyConfig.host()/port()/credentials()` reproduce the old parsing exactly, confirmed in the spec), including `buildProxyConfigurationWithDefaultPort`'s expectation of `port()` returning `-1` when absent (no default-port logic in this class — that's only in Task 8's `AWSAsyncProxyConfigurationProvider`).

- [ ] **Step 3: Run the test suite to verify no regression**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=AWSProxyConfigurationProviderTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 4: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog/aws/AWSProxyConfigurationProvider.java \
        graylog2-server/src/test/java/org/graylog/aws/AWSProxyConfigurationProviderTest.java
git commit -m "Migrate AWSProxyConfigurationProvider to ProxyConfig"
```

---

### Task 8: Migrate `AWSAsyncProxyConfigurationProvider`

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog/aws/AWSAsyncProxyConfigurationProvider.java`
- Modify: `graylog2-server/graylog2-server/src/test/java/org/graylog/aws/AWSAsyncProxyConfigurationProviderTest.java`

**Interfaces:**
- Consumes: `ProxyConfig.scheme()`, `.host()`, `.port(int, int)` (Task 2), `.credentials()` (Task 1).
- Produces: `AWSAsyncProxyConfigurationProvider`'s public API unchanged apart from the constructor parameter type; `buildProxyConfiguration` changes from `static ProxyConfiguration buildProxyConfiguration(URI proxyUri)` to `static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig)`.

Current full file:

```java
package org.graylog.aws;

import com.google.common.base.Splitter;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;

import java.net.URI;
import java.util.List;

/**
 * Provides a Netty-based async HTTP client builder configured with the optional Graylog HTTP proxy
 * (the {@code http_proxy_uri} server configuration).
 * <p>
 * The synchronous {@link AWSProxyConfigurationProvider} uses the Apache HTTP client, which cannot be used by the
 * AWS async clients (DynamoDB, CloudWatch, Kinesis) that the Kinesis input relies on. The Kinesis Client Library
 * additionally requires HTTP/2, which is only supported by the Netty async client. This provider therefore mirrors
 * the proxy logic of {@link AWSProxyConfigurationProvider}, but produces a {@link NettyNioAsyncHttpClient.Builder}.
 */
@Singleton
public class AWSAsyncProxyConfigurationProvider implements Provider<NettyNioAsyncHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAsyncProxyConfigurationProvider.class);
    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private final URI httpProxyUri;

    @Inject
    public AWSAsyncProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public NettyNioAsyncHttpClient.Builder get() {
        final NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();
        if (httpProxyUri == null) {
            LOG.debug("AWS async proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(httpProxyUri));
        LOG.debug("AWS async proxy enabled: {}:{}", httpProxyUri.getHost(), httpProxyUri.getPort());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(URI proxyUri) {
        final String scheme = proxyUri.getScheme();
        final int port = proxyUri.getPort();

        final ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder()
                .scheme(scheme)
                .host(proxyUri.getHost())
                // The Netty proxy configuration requires an explicit port. Fall back to the scheme default when the
                // proxy URI does not specify one.
                .port(port >= 0 ? port : (HTTPS_SCHEME.equalsIgnoreCase(scheme) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT));

        if (proxyUri.getUserInfo() != null && !proxyUri.getUserInfo().isEmpty()) {
            final List<String> credentials = Splitter.on(":")
                    .limit(2)
                    .splitToList(proxyUri.getUserInfo());

            if (credentials.size() == 2) {
                proxyConfigBuilder.username(credentials.get(0));
                proxyConfigBuilder.password(credentials.get(1));
            }
        }

        return proxyConfigBuilder.build();
    }
}
```

Note this class's `ProxyConfiguration` is `software.amazon.awssdk.http.nio.netty.ProxyConfiguration` — a different type from Task 7's `software.amazon.awssdk.http.apache.ProxyConfiguration` despite the identical simple name; this one has no `.endpoint(URI)` method, only discrete `.scheme()`/`.host()`/`.port()`.

- [ ] **Step 1: Replace the whole file**

```java
package org.graylog.aws;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.utilities.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;

/**
 * Provides a Netty-based async HTTP client builder configured with the optional Graylog HTTP proxy
 * (the {@code http_proxy_uri} server configuration).
 * <p>
 * The synchronous {@link AWSProxyConfigurationProvider} uses the Apache HTTP client, which cannot be used by the
 * AWS async clients (DynamoDB, CloudWatch, Kinesis) that the Kinesis input relies on. The Kinesis Client Library
 * additionally requires HTTP/2, which is only supported by the Netty async client. This provider therefore mirrors
 * the proxy logic of {@link AWSProxyConfigurationProvider}, but produces a {@link NettyNioAsyncHttpClient.Builder}.
 */
@Singleton
public class AWSAsyncProxyConfigurationProvider implements Provider<NettyNioAsyncHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAsyncProxyConfigurationProvider.class);
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private final ProxyConfig proxyConfig;

    @Inject
    public AWSAsyncProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public NettyNioAsyncHttpClient.Builder get() {
        final NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();
        if (proxyConfig == null) {
            LOG.debug("AWS async proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(proxyConfig));
        LOG.debug("AWS async proxy enabled: {}:{}", proxyConfig.host(), proxyConfig.port());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig) {
        final ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder()
                .scheme(proxyConfig.scheme())
                .host(proxyConfig.host())
                // The Netty proxy configuration requires an explicit port. Fall back to the scheme default when the
                // proxy URI does not specify one.
                .port(proxyConfig.port(DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT));

        proxyConfig.credentials().ifPresent(credentials ->
                proxyConfigBuilder.username(credentials.username()).password(credentials.password()));

        return proxyConfigBuilder.build();
    }
}
```

(`HTTPS_SCHEME` constant is removed — the scheme comparison now lives inside `ProxyConfig.port(int, int)` itself.)

- [ ] **Step 2: Update the test file's call sites**

Same pattern as Task 7 — wrap each `URI.create(...)` in `new ProxyConfig(...)` across all 7 test methods (`buildProxyConfigurationWithoutCredentials`, `buildProxyConfigurationWithCredentials`, `buildProxyConfigurationWithUsernameOnly`, `buildProxyConfigurationWithHttpsScheme`, `buildProxyConfigurationWithDefaultHttpPort`, `buildProxyConfigurationWithDefaultHttpsPort`), e.g.:

```java
        final ProxyConfig proxyUri = new ProxyConfig(URI.create("http://proxy.example.com:8080"));

        final ProxyConfiguration proxyConfig = AWSAsyncProxyConfigurationProvider.buildProxyConfiguration(proxyUri);
```

Add `import org.graylog2.utilities.ProxyConfig;` to the test file's imports. All assertions stay exactly as-is — `buildProxyConfigurationWithDefaultHttpPort`'s expectation of port `80` and `buildProxyConfigurationWithDefaultHttpsPort`'s expectation of port `443` are the direct regression signal for `ProxyConfig.port(int, int)` (Task 2), now exercised in production code for the first time.

- [ ] **Step 3: Run the test suite to verify no regression**

```bash
./mvnw -o test -pl :graylog2-server -Dtest=AWSAsyncProxyConfigurationProviderTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`.

- [ ] **Step 4: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog/aws/AWSAsyncProxyConfigurationProvider.java \
        graylog2-server/src/test/java/org/graylog/aws/AWSAsyncProxyConfigurationProviderTest.java
git commit -m "Migrate AWSAsyncProxyConfigurationProvider to ProxyConfig"
```

---

### Task 9: Migrate `AWSInstanceNameLookupProcessor`

**Files:**
- Modify: `graylog2-server/graylog2-server/src/main/java/org/graylog/aws/processors/instancelookup/AWSInstanceNameLookupProcessor.java`

**Interfaces:**
- Consumes: `Configuration.getHttpProxyConfig(): Optional<ProxyConfig>` (already exists, unchanged signature — Task 2 of the earlier, already-committed plan). `ProxyConfig.uri(): URI` (record accessor, Task 1).
- Produces: no change to this class's own public API.

This is not a `@Named` injection site — it uses an injected `org.graylog2.Configuration configuration` field and calls the getter. No test file currently exists for this class (confirmed) — this task changes no test.

Current (relevant lines):

```java
                    final HttpUrl proxyUrl = config.proxyEnabled() && configuration.getHttpProxyUri() != null
                            ? HttpUrl.get(configuration.getHttpProxyUri()) : null;
```

- [ ] **Step 1: Update the import block**

Current import block (top of file):

```java
import com.codahale.metrics.MetricRegistry;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.HttpUrl;
import org.graylog.aws.AWS;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.migrations.V20200505121200_EncryptAWSSecretKey;
import org.graylog2.Configuration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
```

Change to:

```java
import com.codahale.metrics.MetricRegistry;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.HttpUrl;
import org.graylog.aws.AWS;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.aws.migrations.V20200505121200_EncryptAWSSecretKey;
import org.graylog2.Configuration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.utilities.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
```

- [ ] **Step 2: Replace the proxy URL computation**

Change:

```java
                    final HttpUrl proxyUrl = config.proxyEnabled() && configuration.getHttpProxyUri() != null
                            ? HttpUrl.get(configuration.getHttpProxyUri()) : null;
```

to:

```java
                    final Optional<ProxyConfig> proxyConfig = configuration.getHttpProxyConfig();
                    final HttpUrl proxyUrl = config.proxyEnabled() && proxyConfig.isPresent()
                            ? HttpUrl.get(proxyConfig.get().uri()) : null;
```

This also fixes the pre-existing double-getter-call code smell (the old code called `configuration.getHttpProxyUri()` twice in one ternary) while the line is touched anyway — not a behavior change, `HttpUrl.get(proxyConfig.get().uri())` reconstructs the identical `HttpUrl` `HttpUrl.get(getHttpProxyUri())` did, since `ProxyConfig.uri()` is exactly the same `URI` `getHttpProxyUri()` used to return.

- [ ] **Step 3: Compile to verify**

```bash
./mvnw -o compile -pl :graylog2-server -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Confirm no remaining callers of the removed `getHttpProxyUri()` anywhere**

```bash
grep -rn "getHttpProxyUri" graylog2-server/src/main/java/ graylog2-server/src/test/java/
```

Expected: no hits at all now (this was the one caller Task 4 Step 5 found and deferred to this task).

- [ ] **Step 5: Commit**

```bash
git add graylog2-server/src/main/java/org/graylog/aws/processors/instancelookup/AWSInstanceNameLookupProcessor.java
git commit -m "Migrate AWSInstanceNameLookupProcessor to ProxyConfig, drop double getter call"
```

---

### Task 10: Migrate `GitRepositoryService` (graylog-plugin-enterprise)

**Files:**
- Modify: `graylog-plugin-enterprise/enterprise/src/main/java/org/graylog/plugins/securityapp/sigma/git/GitRepositoryService.java`

**Interfaces:**
- Consumes: `org.graylog2.utilities.ProxyConfig` (graylog2-server, Task 1).
- Produces: no change to this class's own public API beyond the constructor's last parameter's declared type.

**Prerequisite:** graylog-plugin-enterprise resolves graylog2-server as a Maven dependency from the local repository. Before starting this task, install graylog2-server's changes from Tasks 1-9 locally:

```bash
cd /home/dennis/work/graylog-build-vibeenv-proxy-config-record/.worktrees/proxy-config-record/graylog-project-internal
./mvnw -o install -pl :graylog2-server -am -DskipTests -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Without this, `graylog-plugin-enterprise` compiles against a stale graylog2-server jar and won't see the new `ProxyConfig.port(int, int)`, the removed `ProxyConfig.from(URI)`, or `ProxySelectorProvider`'s new constructor signature.

This is the lowest-risk consumer in the whole plan: the injected value is only ever null-checked, never read for host/port/scheme/credentials.

Current constructor (lines 100-119) and both usage sites:

```java
    @Inject
    public GitRepositoryService(DBGitRepositoryService dbRepoService,
                                SigmaRuleParser sigmaRuleParser,
                                GridFSFileService gridFSFileService,
                                SigmaImportService importService,
                                ProxySelectorProvider proxySelectorProvider,
                                EncryptedValueService encryptedValueService,
                                DBEventDefinitionService eventDefinitionService,
                                @Named(GIT_REPOSITORY_DIR) @Nullable String repositoryDir,
                                @Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.dbRepoService = dbRepoService;
        this.sigmaRuleParser = sigmaRuleParser;
        this.gridFSFileService = gridFSFileService;
        this.importService = importService;
        this.proxySelectorProvider = proxySelectorProvider;
        this.encryptedValueService = encryptedValueService;
        this.eventDefinitionService = eventDefinitionService;
        this.repositoryDir = repositoryDir;
        this.httpProxyUri = httpProxyUri;
    }
```

Field declaration (line 98): `private final URI httpProxyUri;`. Both usage sites (elsewhere in the file): `if (httpProxyUri != null) { ProxySelector.setDefault(proxySelectorProvider.get()); }` (around a JGit `CloneCommand`) and the matching restore in a `finally` block.

- [ ] **Step 1: Update the import block**

Current import block (top of file):

```java
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.plugins.files.FileOperationException;
import org.graylog.plugins.files.impl.GridFSFileService;
import org.graylog.plugins.securityapp.sigma.SigmaRuleParsingException;
import org.graylog.plugins.securityapp.sigma.imports.SharedOverrides;
import org.graylog.plugins.securityapp.sigma.imports.SigmaImportResult;
import org.graylog.plugins.securityapp.sigma.imports.SigmaImportService;
import org.graylog.plugins.securityapp.sigma.processor.SigmaEventProcessorConfig;
import org.graylog.plugins.securityapp.sigma.processor.correlation.SigmaCorrelationEventProcessorConfig;
import org.graylog.plugins.securityapp.sigma.rest.CreateUpdateRepoRequest;
import org.graylog.plugins.securityapp.sigma.rest.GitRepositoryResponse;
import org.graylog.plugins.securityapp.sigma.rest.ImportAllRulesRequest;
import org.graylog.plugins.securityapp.sigma.rest.RepositoryCreateUpdateResponse;
import org.graylog.plugins.securityapp.sigma.rest.RuleImportResponse;
import org.graylog.plugins.securityapp.sigma.rules.SigmaRule;
import org.graylog.plugins.securityapp.sigma.rules.SigmaRuleParser;
import org.graylog.plugins.securityapp.sigma.rules.correlation.SigmaCorrelationRule;
import org.graylog.plugins.securityapp.sigma.rules.detection.SigmaDetectionRule;
import org.graylog.security.UserContext;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ProxySelectorProvider;
import org.graylog2.shared.utilities.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
```

Change to (`java.net.URI` removed — confirmed unused elsewhere in this file, the only other occurrence of the string "URI" is inside a comment; `org.graylog2.utilities.ProxyConfig` added after `org.graylog2.shared.utilities.StringUtils`, before `org.joda.time.DateTime`):

```java
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.plugins.files.FileOperationException;
import org.graylog.plugins.files.impl.GridFSFileService;
import org.graylog.plugins.securityapp.sigma.SigmaRuleParsingException;
import org.graylog.plugins.securityapp.sigma.imports.SharedOverrides;
import org.graylog.plugins.securityapp.sigma.imports.SigmaImportResult;
import org.graylog.plugins.securityapp.sigma.imports.SigmaImportService;
import org.graylog.plugins.securityapp.sigma.processor.SigmaEventProcessorConfig;
import org.graylog.plugins.securityapp.sigma.processor.correlation.SigmaCorrelationEventProcessorConfig;
import org.graylog.plugins.securityapp.sigma.rest.CreateUpdateRepoRequest;
import org.graylog.plugins.securityapp.sigma.rest.GitRepositoryResponse;
import org.graylog.plugins.securityapp.sigma.rest.ImportAllRulesRequest;
import org.graylog.plugins.securityapp.sigma.rest.RepositoryCreateUpdateResponse;
import org.graylog.plugins.securityapp.sigma.rest.RuleImportResponse;
import org.graylog.plugins.securityapp.sigma.rules.SigmaRule;
import org.graylog.plugins.securityapp.sigma.rules.SigmaRuleParser;
import org.graylog.plugins.securityapp.sigma.rules.correlation.SigmaCorrelationRule;
import org.graylog.plugins.securityapp.sigma.rules.detection.SigmaDetectionRule;
import org.graylog.security.UserContext;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ProxySelectorProvider;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.utilities.ProxyConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ProxySelector;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
```

- [ ] **Step 2: Change the field and constructor**

Change the field declaration:

```java
    private final ProxyConfig httpProxyConfig;
```

Change the constructor's last parameter and its assignment:

```java
    @Inject
    public GitRepositoryService(DBGitRepositoryService dbRepoService,
                                SigmaRuleParser sigmaRuleParser,
                                GridFSFileService gridFSFileService,
                                SigmaImportService importService,
                                ProxySelectorProvider proxySelectorProvider,
                                EncryptedValueService encryptedValueService,
                                DBEventDefinitionService eventDefinitionService,
                                @Named(GIT_REPOSITORY_DIR) @Nullable String repositoryDir,
                                @Named("http_proxy_uri") @Nullable ProxyConfig httpProxyConfig) {
        this.dbRepoService = dbRepoService;
        this.sigmaRuleParser = sigmaRuleParser;
        this.gridFSFileService = gridFSFileService;
        this.importService = importService;
        this.proxySelectorProvider = proxySelectorProvider;
        this.encryptedValueService = encryptedValueService;
        this.eventDefinitionService = eventDefinitionService;
        this.repositoryDir = repositoryDir;
        this.httpProxyConfig = httpProxyConfig;
    }
```

- [ ] **Step 3: Update both usage sites**

Both occurrences of `if (httpProxyUri != null)` become `if (httpProxyConfig != null)` — same field, renamed, same null-check semantics, no other change.

- [ ] **Step 4: Run the existing test suite to verify no regression**

From the meta-project root:

```bash
./mvnw -o test -pl :graylog-plugin-enterprise -am -Dtest=GitRepositoryServiceTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: all existing tests pass unchanged. `GitRepositoryServiceTest` constructs the service with a literal `null` for this parameter (`new GitRepositoryService(..., null)`) — `null` is assignable to `ProxyConfig` exactly as it was to `URI`, so this test file needs **no edits at all**; confirm this by reading it, don't skip the confirmation just because no edit seems needed.

- [ ] **Step 5: Commit**

```bash
git add enterprise/src/main/java/org/graylog/plugins/securityapp/sigma/git/GitRepositoryService.java
git commit -m "Migrate GitRepositoryService to ProxyConfig"
```

---

### Task 11: Restore proxy support in `ChatModelFactory` (graylog-plugin-enterprise)

**Files:**
- Modify: `graylog-plugin-enterprise/enterprise/src/main/java/org/graylog/plugins/aiagent/runtime/lc4j/ChatModelFactory.java`
- Modify: `graylog-plugin-enterprise/enterprise/src/test/java/org/graylog/plugins/aiagent/runtime/lc4j/ChatModelFactoryTest.java`

**Interfaces:**
- Consumes: `org.graylog2.utilities.ProxyConfig` — `.credentials()`, `.endpoint()` (graylog2-server, Task 1); `org.graylog2.shared.bindings.providers.ProxySelectorProvider` — `.get(): ProxySelector` (graylog2-server, Task 5's revised constructor, but this class only calls `.get()`, unaffected by the constructor signature change).
- Produces: `ChatModelFactory`'s public API changes — constructor becomes `(ProxySelectorProvider, @Named("http_proxy_uri") @Nullable ProxyConfig)` (currently no-arg); `create(ModelSpec): CloseableChatModel` is unchanged. New package-private `jdkHttpClientBuilder(): HttpClientBuilder` and `bedrockProxyConfiguration(): Optional<software.amazon.awssdk.http.apache5.ProxyConfiguration>`.

**Prerequisite:** same as Task 10 — graylog2-server must already be `mvn install`ed locally (Task 10's prerequisite step covers this; if Tasks 10 and 11 are done in the same session, no need to repeat it, but confirm the jar timestamp reflects Task 9's changes before proceeding).

This is the one genuinely new-functionality task in this plan — unlike Tasks 5-10, it is not re-establishing prior behavior, it is adding proxy support that (per investigation) does not currently exist anywhere in the AI agent runtime.

Current full `ChatModelFactory.java`:

```java
package org.graylog.plugins.aiagent.runtime.lc4j;

import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.inject.Singleton;
import org.graylog.plugins.aiagent.modelprovider.ModelProvider;
import org.graylog.plugins.aiagent.modelprovider.client.ResolvedCredentials;
import org.graylog.plugins.aiagent.runtime.ModelSpec;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

import java.time.Duration;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class ChatModelFactory {

    private static final int MAX_OUTPUT_TOKENS = 8192;

    private static final Duration TIMEOUT = Duration.ofMinutes(2);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 3;

    public CloseableChatModel create(ModelSpec spec) {
        return switch (spec.provider()) {
            case ANTHROPIC -> CloseableChatModel.nothingToClose(anthropic(spec));
            case BEDROCK -> bedrock(spec);
            case OPENAI -> CloseableChatModel.nothingToClose(openAi(spec));
            case OLLAMA -> CloseableChatModel.nothingToClose(ollama(spec));
            case GOOGLE -> CloseableChatModel.nothingToClose(google(spec));
        };
    }

    private ChatModel openAi(ModelSpec spec) {
        return OpenAiChatModel.builder()
                .apiKey(requireApiKey(spec))
                .modelName(spec.modelId())
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }

    private ChatModel ollama(ModelSpec spec) {
        if (!(spec.credentials() instanceof ResolvedCredentials.Ollama(String baseUrl))) {
            throw new IllegalArgumentException("Ollama needs an Ollama base URL, got " + spec.credentials());
        }
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(spec.modelId())
                .truncate(false)
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }

    private ChatModel google(ModelSpec spec) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(requireApiKey(spec))
                .modelName(spec.modelId())
                .returnThinking(true)
                .sendThinking(true)
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }

    private ChatModel anthropic(ModelSpec spec) {
        return AnthropicChatModel.builder()
                .apiKey(requireApiKey(spec))
                .modelName(spec.modelId())
                .maxTokens(MAX_OUTPUT_TOKENS)
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }

    private CloseableChatModel bedrock(ModelSpec spec) {
        if (!(spec.credentials() instanceof ResolvedCredentials.Bedrock credentials)) {
            throw new IllegalArgumentException("Bedrock needs Bedrock credentials, got " + spec.credentials());
        }
        final String region = credentials.region();

        final BedrockRuntimeClientBuilder clientBuilder =
                BedrockRuntimeClient.builder().region(Region.of(region));
        final var authInterceptors = BedrockAuth.configureCredentials(clientBuilder, credentials);

        final BedrockRuntimeClient client = clientBuilder
                .httpClientBuilder(Apache5HttpClient.builder()
                        .socketTimeout(TIMEOUT)
                        .connectionTimeout(CONNECT_TIMEOUT))
                .overrideConfiguration(override -> {
                    authInterceptors.forEach(override::addExecutionInterceptor);
                    override.retryStrategy(AwsRetryStrategy.doNotRetry());
                })
                .build();

        return new CloseableChatModel(BedrockChatModel.builder()
                .client(client)
                .modelId(spec.modelId())
                .maxRetries(MAX_RETRIES)
                .build(), client);
    }

    private static String requireApiKey(ModelSpec spec) {
        if (spec.credentials() instanceof ResolvedCredentials.ApiKey(String key) && key != null && !key.isBlank()) {
            return key;
        }
        throw new IllegalArgumentException(f("Missing API key for provider %s", spec.provider().id()));
    }
}
```

- [ ] **Step 1: Write the failing tests**

Add these to `ChatModelFactoryTest.java`, alongside the two existing tests (`bedrockHandsBackTheClientToClose`, `theApiKeyProvidersHaveNothingToClose`) — do not modify those two yet, that's Step 2. These new tests call the two new package-private methods directly (white-box, same style the class's existing tests already use for `create(...)`):

```java
    @Test
    void jdkHttpClientBuilderRoutesThroughTheConfiguredProxy() {
        final ProxyConfig proxyConfig = new ProxyConfig(URI.create("http://203.0.113.10:8123"));
        final ChatModelFactory factory = new ChatModelFactory(
                new ProxySelectorProvider(proxyConfig, null), proxyConfig);

        final HttpClient.Builder builder = ((JdkHttpClientBuilder) factory.jdkHttpClientBuilder()).httpClientBuilder();

        assertThat(builder.build().proxy()).isPresent();
    }

    @Test
    void jdkHttpClientBuilderFallsBackToTheJvmDefaultWithoutAConfiguredProxy() {
        final ChatModelFactory factory = new ChatModelFactory(new ProxySelectorProvider(null, null), null);

        final HttpClient.Builder builder = ((JdkHttpClientBuilder) factory.jdkHttpClientBuilder()).httpClientBuilder();

        assertThat(builder.build().proxy()).isEmpty();
    }

    @Test
    void bedrockProxyConfigurationIsAbsentWithoutAConfiguredProxy() {
        final ChatModelFactory factory = new ChatModelFactory(new ProxySelectorProvider(null, null), null);

        assertThat(factory.bedrockProxyConfiguration()).isEmpty();
    }

    @Test
    void bedrockProxyConfigurationCarriesHostPortSchemeAndCredentials() {
        final ProxyConfig proxyConfig = new ProxyConfig(URI.create("http://someuser:somepass@203.0.113.10:8123"));
        final ChatModelFactory factory = new ChatModelFactory(
                new ProxySelectorProvider(proxyConfig, null), proxyConfig);

        final ProxyConfiguration bedrockProxy = factory.bedrockProxyConfiguration().orElseThrow();

        assertThat(bedrockProxy.host()).isEqualTo("203.0.113.10");
        assertThat(bedrockProxy.port()).isEqualTo(8123);
        assertThat(bedrockProxy.scheme()).isEqualTo("http");
        assertThat(bedrockProxy.username()).isEqualTo("someuser");
        assertThat(bedrockProxy.password()).isEqualTo("somepass");
    }
```

Current import block (top of file):

```java
package org.graylog.plugins.aiagent.runtime.lc4j;

import dev.langchain4j.model.bedrock.BedrockChatModel;
import org.graylog.plugins.aiagent.modelprovider.ModelProvider;
import org.graylog.plugins.aiagent.modelprovider.client.ResolvedCredentials;
import org.graylog.plugins.aiagent.runtime.ModelSpec;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import static org.assertj.core.api.Assertions.assertThat;
```

Change to (adding a new `java.*` import block, since none currently exists in this file):

```java
package org.graylog.plugins.aiagent.runtime.lc4j;

import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import org.graylog.plugins.aiagent.modelprovider.ModelProvider;
import org.graylog.plugins.aiagent.modelprovider.client.ResolvedCredentials;
import org.graylog.plugins.aiagent.runtime.ModelSpec;
import org.graylog2.shared.bindings.providers.ProxySelectorProvider;
import org.graylog2.utilities.ProxyConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache5.ProxyConfiguration;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import java.net.URI;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
```

(`203.0.113.10` — a documentation-only RFC 5737 address, no live DNS needed — used here for the same reason it replaced `www.example.com` in `OkHttpClientProviderTest`, Task 1: any test target address should never depend on real network resolution.)

- [ ] **Step 2: Run the tests to verify they fail**

From the meta-project root:

```bash
cd /home/dennis/work/graylog-build-vibeenv-proxy-config-record/.worktrees/proxy-config-record/graylog-project-internal
./mvnw -o test -pl :graylog-plugin-enterprise -am -Dtest=ChatModelFactoryTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: compile failure — `new ChatModelFactory(...)` with two arguments doesn't match the current no-arg constructor; `jdkHttpClientBuilder()`/`bedrockProxyConfiguration()` don't exist yet.

- [ ] **Step 3: Update the constructor and existing tests' construction calls**

In `ChatModelFactory.java`, current import block (top of file):

```java
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.inject.Singleton;
import org.graylog.plugins.aiagent.modelprovider.ModelProvider;
import org.graylog.plugins.aiagent.modelprovider.client.ResolvedCredentials;
import org.graylog.plugins.aiagent.runtime.ModelSpec;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

import java.time.Duration;

import static org.graylog2.shared.utilities.StringUtils.f;
```

Change to (this repo's actual convention, confirmed against `ProxySelectorProvider.java`/`ProxyConfig.java`, places `javax.*` before `java.*` in that shared block, ahead of a literal alphabetical reading):

```java
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.plugins.aiagent.modelprovider.ModelProvider;
import org.graylog.plugins.aiagent.modelprovider.client.ResolvedCredentials;
import org.graylog.plugins.aiagent.runtime.ModelSpec;
import org.graylog2.shared.bindings.providers.ProxySelectorProvider;
import org.graylog2.utilities.ProxyConfig;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

import javax.annotation.Nullable;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;
```

Add the constructor (immediately after the class's field declarations, before `create(...)`):

```java
    private final ProxySelectorProvider proxySelectorProvider;
    private final Optional<ProxyConfig> proxyConfig;

    @Inject
    public ChatModelFactory(ProxySelectorProvider proxySelectorProvider,
                             @Named("http_proxy_uri") @Nullable ProxyConfig proxyConfig) {
        this.proxySelectorProvider = proxySelectorProvider;
        this.proxyConfig = Optional.ofNullable(proxyConfig);
    }
```

In `ChatModelFactoryTest.java`, the class currently has this field (both existing test methods use it via `factory.create(...)`):

```java
    private final ChatModelFactory factory = new ChatModelFactory();
```

Change to:

```java
    private final ChatModelFactory factory = new ChatModelFactory(new ProxySelectorProvider(null, null), null);
```

- [ ] **Step 4: Add `jdkHttpClientBuilder()` and apply it to the four langchain4j providers**

Add this private helper (after the field/constructor, before `create(...)`, or immediately after `create(...)` — match the file's existing method ordering convention):

```java
    HttpClientBuilder jdkHttpClientBuilder() {
        final HttpClient.Builder builder = HttpClient.newBuilder().proxy(proxySelectorProvider.get());
        proxyConfig.flatMap(ProxyConfig::credentials).ifPresent(credentials ->
                builder.authenticator(proxyOnlyAuthenticator(credentials.username(), credentials.password())));
        return new JdkHttpClientBuilder().httpClientBuilder(builder);
    }

    private static java.net.Authenticator proxyOnlyAuthenticator(String username, String password) {
        return new java.net.Authenticator() {
            @Override
            protected java.net.PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() != RequestorType.PROXY) {
                    return null;
                }
                return new java.net.PasswordAuthentication(username, password.toCharArray());
            }
        };
    }
```

(Package-private, not `private`, to match the brief's "Produces" interface and let the test call it directly — same visibility pattern the original, superseded Task 5 draft used.)

Add `.httpClientBuilder(jdkHttpClientBuilder())` to each of the four builder chains:

In `openAi(ModelSpec spec)`:

```java
    private ChatModel openAi(ModelSpec spec) {
        return OpenAiChatModel.builder()
                .apiKey(requireApiKey(spec))
                .modelName(spec.modelId())
                .httpClientBuilder(jdkHttpClientBuilder())
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }
```

In `ollama(ModelSpec spec)`:

```java
    private ChatModel ollama(ModelSpec spec) {
        if (!(spec.credentials() instanceof ResolvedCredentials.Ollama(String baseUrl))) {
            throw new IllegalArgumentException("Ollama needs an Ollama base URL, got " + spec.credentials());
        }
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(spec.modelId())
                .httpClientBuilder(jdkHttpClientBuilder())
                .truncate(false)
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }
```

In `google(ModelSpec spec)`:

```java
    private ChatModel google(ModelSpec spec) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(requireApiKey(spec))
                .modelName(spec.modelId())
                .httpClientBuilder(jdkHttpClientBuilder())
                .returnThinking(true)
                .sendThinking(true)
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }
```

In `anthropic(ModelSpec spec)`:

```java
    private ChatModel anthropic(ModelSpec spec) {
        return AnthropicChatModel.builder()
                .apiKey(requireApiKey(spec))
                .modelName(spec.modelId())
                .httpClientBuilder(jdkHttpClientBuilder())
                .maxTokens(MAX_OUTPUT_TOKENS)
                .timeout(TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }
```

(Applied uniformly to all four, including Ollama's typically-internal host — `ProxySelectorProvider` already bypasses loopback/non-proxy-hosts correctly on its own, so there's no reason to special-case any provider here, per the spec.)

- [ ] **Step 5: Add `bedrockProxyConfiguration()` and apply it in `bedrock()`**

Add this private helper:

```java
    private Optional<software.amazon.awssdk.http.apache5.ProxyConfiguration> bedrockProxyConfiguration() {
        return proxyConfig.map(config -> {
            final var builder = software.amazon.awssdk.http.apache5.ProxyConfiguration.builder()
                    .endpoint(config.endpoint());
            config.credentials().ifPresent(credentials ->
                    builder.username(credentials.username()).password(credentials.password()));
            return builder.build();
        });
    }
```

In `bedrock(ModelSpec spec)`, change:

```java
        final BedrockRuntimeClient client = clientBuilder
                .httpClientBuilder(Apache5HttpClient.builder()
                        .socketTimeout(TIMEOUT)
                        .connectionTimeout(CONNECT_TIMEOUT))
                .overrideConfiguration(override -> {
                    authInterceptors.forEach(override::addExecutionInterceptor);
                    override.retryStrategy(AwsRetryStrategy.doNotRetry());
                })
                .build();
```

to:

```java
        final Apache5HttpClient.Builder httpClientBuilder = Apache5HttpClient.builder()
                .socketTimeout(TIMEOUT)
                .connectionTimeout(CONNECT_TIMEOUT);
        bedrockProxyConfiguration().ifPresent(httpClientBuilder::proxyConfiguration);

        final BedrockRuntimeClient client = clientBuilder
                .httpClientBuilder(httpClientBuilder)
                .overrideConfiguration(override -> {
                    authInterceptors.forEach(override::addExecutionInterceptor);
                    override.retryStrategy(AwsRetryStrategy.doNotRetry());
                })
                .build();
```

No change to `BedrockAuth.configureCredentials(clientBuilder, credentials)` — it operates on the separate `BedrockRuntimeClientBuilder`, not the HTTP client builder, confirmed no interaction.

- [ ] **Step 6: Run the test suite to verify it passes**

```bash
./mvnw -o test -pl :graylog-plugin-enterprise -am -Dtest=ChatModelFactoryTest -Dsurefire.failIfNoSpecifiedTests=false -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

Expected: `Tests run: 6, Failures: 0, Errors: 0` (2 existing + 4 new) — this real Maven command has been verified to work in this environment throughout this whole effort; no javac/JUnit-Platform-Launcher fallback should be needed.

- [ ] **Step 7: Commit**

```bash
git add enterprise/src/main/java/org/graylog/plugins/aiagent/runtime/lc4j/ChatModelFactory.java \
        enterprise/src/test/java/org/graylog/plugins/aiagent/runtime/lc4j/ChatModelFactoryTest.java
git commit -m "Restore proxy support to ChatModelFactory's AI agent HTTP clients"
```
