# `ProxyConfigConverter`: making `http_proxy_uri` a `ProxyConfig` everywhere

## Context

`2026-09-03-proxy-config-record.md` introduced `org.graylog2.utilities.ProxyConfig`
(graylog2-server), a value type wrapping `http_proxy_uri`'s parsed `URI` with
`host()`, `port()`, `scheme()`, `endpoint()` (userinfo stripped) and
`credentials()` (parsed `user:password`). Tasks 1-4 of that plan are done,
reviewed, and committed on branch `proxy-config-record` (both
`graylog2-server` and `graylog-plugin-enterprise` repos, same branch name):
`ProxyConfig` itself, a `BaseConfiguration.getHttpProxyConfig()` convenience
getter, and migrating `ProxySelectorProvider`/`OkHttpClientProvider` to build
an `Optional<ProxyConfig>` from their injected `@Named("http_proxy_uri")
@Nullable URI` at construction time.

That plan's Task 5 (migrate `ChatModelFactory` in `graylog-plugin-enterprise`)
turned out to target code that no longer exists: `ChatModelFactory` was
refactored to a `create(ModelSpec)` API with zero proxy-handling logic
anywhere in it. Investigating that surfaced two separate, larger questions,
each resolved in conversation before this spec:

1. **Should AI-agent-runtime HTTP clients respect `http_proxy_uri` at all?**
   Answer: yes, this is an accidental gap, not an intentional removal —
   restore it (see "ChatModelFactory" component below).
2. **Should `ProxyConfig` be directly Guice-injectable**, instead of every
   consumer repeating `@Named("http_proxy_uri") @Nullable URI` +
   `ProxyConfig.from(uri)`? Investigating this surfaced that `@Named("http_proxy_uri")`
   is consumed as a raw `URI` by two more classes the original plan
   explicitly protected as out-of-scope (`AWSProxyConfigurationProvider`,
   `AWSAsyncProxyConfigurationProvider`), plus one more in
   `graylog-plugin-enterprise` (`GitRepositoryService`) and one getter-based
   consumer (`AWSInstanceNameLookupProcessor`). The answer settled on: yes,
   via a JadConfig `Converter` (the same mechanism already used for
   `http_non_proxy_hosts` → `ProxyHostsPattern`), applied to every consumer,
   including the ones the original plan exempted.

This spec covers that whole sweep, superseding the relevant parts of
`2026-09-03-proxy-config-record.md` (Tasks 3 and 4 are revised, not
reverted; Task 5 is redefined). It's one spec because every component here
depends on the same foundational change (`http_proxy_uri` becoming a
`ProxyConfig`), and sequencing it as one plan avoids doing `ChatModelFactory`
twice — once against the old per-consumer `.from(uri)` pattern, once against
the new injected-`ProxyConfig` pattern.

## Goals

- `@Named("http_proxy_uri")` is bound as `ProxyConfig` (nullable) everywhere
  it's injected, in both repos, with no consumer left doing its own
  `URI` → proxy-fields parsing.
- Restore proxy support to the AI agent runtime's outbound HTTP clients
  (OpenAI, Anthropic, Google, Ollama via langchain4j; Bedrock via the AWS
  SDK's Apache5 client), built directly on `ProxyConfig` from the start.
- No change to `graylog.conf`'s `http_proxy_uri` format or semantics — same
  raw string, just parsed one step further before it reaches consumers.
- Preserve every existing consumer's actual behavior exactly (including the
  quirky bits: `AWSAsyncProxyConfigurationProvider`'s 80/443 default-port
  fallback, `AWSProxyConfigurationProvider`'s userinfo-stripped endpoint
  reconstruction) — this is a refactor for six of the seven touched
  consumers, and new functionality for only one (`ChatModelFactory`).

## Non-goals

- No change to `http_non_proxy_hosts`/`ProxyHostsPattern` (unchanged,
  stays a separate constructor parameter wherever both are needed, per
  `ProxyConfig`'s existing, committed design).
- No change to any consumer's own client-specific proxy object construction
  beyond swapping their raw-`URI` parsing for `ProxyConfig` accessors — e.g.
  `AWSAsyncProxyConfigurationProvider` still builds a
  `software.amazon.awssdk.http.nio.netty.ProxyConfiguration`, it just stops
  hand-rolling the parsing that produces its inputs.
- `AWSInstanceNameLookupProcessor`'s `config.proxyEnabled()` flag (a
  separate, unrelated `AWSPluginConfiguration` cluster-config setting) is
  untouched.
- `AzureProxyOptionsFactory` (mentioned as out-of-scope in the original
  plan): still out of scope. It was never found consuming
  `@Named("http_proxy_uri")` in the investigation for this spec either —
  worth a quick existence/relevance check at implementation time, but no
  design decision needed here.

## Component 1: `ProxyConfigConverter` and `BaseConfiguration`

New `org.graylog2.utilities.ProxyConfigConverter implements
Converter<ProxyConfig>` (`graylog2-server`), mirroring the existing
`ProxyHostsPatternConverter` exactly:

```java
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

JadConfig invokes a `Converter`'s `convertFrom` whenever the key is present
in the configuration at all, including with an empty value (`http_proxy_uri =`
with nothing after the `=` still calls `convertFrom("")`); it's only skipped
when the key is absent entirely — in that case, when `http_proxy_uri` is
unset, the field simply stays at its default (`null`), the converter never
runs, exactly like every other optional `@Parameter` in `BaseConfiguration`
today.

`BaseConfiguration.java` (`org.graylog2.plugin`), current state:

```java
@Parameter(value = "http_proxy_uri")
private URI httpProxyUri;
...
public URI getHttpProxyUri() {
    return httpProxyUri;
}

public Optional<ProxyConfig> getHttpProxyConfig() {
    return ProxyConfig.from(httpProxyUri);
}
```

Becomes:

```java
@Parameter(value = "http_proxy_uri", converter = ProxyConfigConverter.class)
private ProxyConfig httpProxyConfig;
...
public Optional<ProxyConfig> getHttpProxyConfig() {
    return Optional.ofNullable(httpProxyConfig);
}
```

`getHttpProxyUri()` is removed. `ProxyConfig.from(URI)` (the static factory
Task 1 added) has no remaining caller once every consumer below is migrated
— remove it as dead code (and its two `ProxyConfigTest` cases that test it
directly: `fromReturnsEmptyWithoutAConfiguredProxy`,
`fromWrapsAConfiguredProxy`).

`ProxyConfig` gains one new accessor for the port-fallback case (Component
2 below):

```java
public int port(int httpDefault, int httpsDefault) {
    final int port = port();
    return port >= 0 ? port : ("https".equalsIgnoreCase(scheme()) ? httpsDefault : httpDefault);
}
```

`port()` itself is untouched — still a plain, unopinionated passthrough of
`URI.getPort()`, as already committed and reviewed in Task 1.

**JadConfig-Guice binding mechanics, precisely:** JadConfig's
`NamedConfigParametersModule` binds each `@Parameter` field under its
`@Named` value using the field's declared type — there is no native
`Optional<T>` support in JadConfig (confirmed: no `Optional`-related class
anywhere in the `jadconfig` jar). So `@Named("http_proxy_uri")` becomes
injectable as `@Nullable ProxyConfig` (not literally `java.util.Optional<ProxyConfig>`)
— exactly the same shape `@Named("http_non_proxy_hosts") @Nullable
ProxyHostsPattern` already has today. Every consumer below wraps it locally
as `Optional.ofNullable(...)` when that's useful internally, same as
`ProxySelectorProvider`/`OkHttpClientProvider` already do.

Because Guice binding keys are (type, annotation) pairs, this binding
coexists with nothing else under the same name — there was never a
separate `@Named("http_proxy_uri") URI` binding to conflict with once the
`BaseConfiguration` field's declared type changes; the old `URI` binding is
replaced, not shadowed. This is why every direct consumer of the raw `URI`
must be migrated in the same change — an un-migrated consumer would fail to
compile (its `@Named("http_proxy_uri") @Nullable URI` constructor parameter
would have nothing bound to it).

## Component 2: consumer migrations

All seven consumers change their constructor parameter from
`@Named("http_proxy_uri") @Nullable URI` to `@Named("http_proxy_uri")
@Nullable ProxyConfig`. What each one does with it:

### 2a. `ProxySelectorProvider` (graylog2-server, revises Task 3)

Already stores `Optional<ProxyConfig> proxyConfig` via `ProxyConfig.from(httpProxyUri)`
in its constructor. Change: drop the `.from(...)` call, take `ProxyConfig`
directly, wrap with `Optional.ofNullable(...)`. `get()`/`getProxyAddress()`
are unchanged (they already operate on `proxyConfig`, not the raw URI).
Global constraint from the original plan still holds: `getProxyAddress()`
stays `public`, `select()` still calls `this.getProxyAddress()` (the
`OkHttpClientProviderTest#testDynamicProxy` spy assertion depends on this).

### 2b. `OkHttpClientProvider` (graylog2-server, revises Task 4)

Same pattern as 2a — drop `ProxyConfig.from(...)`, take `ProxyConfig`
directly.

### 2c. `AWSProxyConfigurationProvider` (graylog2-server, newly in scope)

Current `buildProxyConfiguration(URI proxyUri)` (static, package-visible)
hand-reconstructs a userinfo-stripped `URI` for
`software.amazon.awssdk.http.apache.ProxyConfiguration.Builder#endpoint`
and hand-splits userinfo for username/password — both already done by
`ProxyConfig.endpoint()`/`.credentials()`. New signature:
`static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig)`:

```java
static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig) {
    final ProxyConfiguration.Builder builder = ProxyConfiguration.builder()
            .endpoint(proxyConfig.endpoint());
    proxyConfig.credentials().ifPresent(credentials ->
            builder.username(credentials.username()).password(credentials.password()));
    return builder.build();
}
```

One accepted behavior difference: the current hand-rolled endpoint
reconstruction preserves the URI's path/query/fragment (via `new
URI(scheme, null, host, port, path, query, fragment)`); `ProxyConfig.endpoint()`
does not (`f("%s://%s:%d", scheme(), host(), port())`, already committed and
reviewed in Task 1). `http_proxy_uri` is documented and always used as a bare
`scheme://[user:pass@]host:port` — no real configuration has ever set a
path/query/fragment on it — so this is a non-behavior-changing
simplification in practice, not a regression to design around.

### 2d. `AWSAsyncProxyConfigurationProvider` (graylog2-server, newly in scope)

Current `buildProxyConfiguration(URI proxyUri)` hand-computes scheme, host,
and a defaulted port (`port >= 0 ? port : (https ? 443 : 80)`), and
hand-splits userinfo. New signature: `static ProxyConfiguration
buildProxyConfiguration(ProxyConfig proxyConfig)`, using the new
`ProxyConfig.port(int, int)` from Component 1:

```java
static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig) {
    final ProxyConfiguration.Builder builder = ProxyConfiguration.builder()
            .scheme(proxyConfig.scheme())
            .host(proxyConfig.host())
            .port(proxyConfig.port(DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT));
    proxyConfig.credentials().ifPresent(credentials ->
            builder.username(credentials.username()).password(credentials.password()));
    return builder.build();
}
```

(`DEFAULT_HTTP_PORT`/`DEFAULT_HTTPS_PORT` constants stay as they are today,
just passed as arguments instead of used inline.) Note this class's
`ProxyConfiguration` type is `software.amazon.awssdk.http.nio.netty.ProxyConfiguration`
— a different class from 2c's `software.amazon.awssdk.http.apache.ProxyConfiguration`,
despite the identical simple name; no `.endpoint()` method on this one, it's
discrete scheme/host/port fields only.

### 2e. `GitRepositoryService` (graylog-plugin-enterprise, newly in scope)

Lowest-risk of the whole set: the injected value is only ever null-checked
(`if (httpProxyUri != null) { ProxySelector.setDefault(proxySelectorProvider.get()); }`,
and the matching restore in a `finally` block) to decide whether to
temporarily override the JVM-wide default `ProxySelector` around a JGit
`CloneCommand`. Field/parameter type changes from `URI` to `ProxyConfig`;
both `!= null` checks are otherwise untouched.

### 2f. `AWSInstanceNameLookupProcessor` (graylog2-server, getter-based, newly in scope)

Not a `@Named` injection site — takes an injected `org.graylog2.Configuration
configuration` field and currently calls `configuration.getHttpProxyUri()`
twice in one ternary:

```java
final HttpUrl proxyUrl = config.proxyEnabled() && configuration.getHttpProxyUri() != null
        ? HttpUrl.get(configuration.getHttpProxyUri()) : null;
```

Becomes one cached call to the getter that still exists after this change
(`getHttpProxyConfig()`), fixing the double-getter-call smell while the
line is touched anyway:

```java
final Optional<ProxyConfig> proxyConfig = configuration.getHttpProxyConfig();
final HttpUrl proxyUrl = config.proxyEnabled() && proxyConfig.isPresent()
        ? HttpUrl.get(proxyConfig.get().uri()) : null;
```

### 2g. `ChatModelFactory` (graylog-plugin-enterprise, new functionality)

Restores proxy support for the AI agent runtime's outbound HTTP clients,
built directly on the injected `ProxyConfig` (no interim `.from(uri)` step,
unlike 2a/2b which are migrating existing code).

Constructor gains two new parameters — `ProxySelectorProvider` (for the
actual `java.net.ProxySelector`) and `@Named("http_proxy_uri") @Nullable
ProxyConfig` (for credentials/endpoint, needed by both the JDK path's
authenticator and Bedrock's AWS `ProxyConfiguration`):

```java
private final ProxySelectorProvider proxySelectorProvider;
private final Optional<ProxyConfig> proxyConfig;

@Inject
public ChatModelFactory(ProxySelectorProvider proxySelectorProvider,
                         @Named("http_proxy_uri") @Nullable ProxyConfig httpProxyConfig) {
    this.proxySelectorProvider = proxySelectorProvider;
    this.proxyConfig = Optional.ofNullable(httpProxyConfig);
}
```

Guice can JIT-bind `ProxySelectorProvider` (its own `@Inject` constructor's
dependencies — `http_proxy_uri`, `http_non_proxy_hosts` — are already bound
core config), so no new Guice binding is needed for this parameter, same as
today's existing injection of `ProxySelectorProvider` elsewhere (e.g.
`OkHttpClientProvider`, `GitRepositoryService`).

New private helper, applied uniformly to all four langchain4j-backed
providers (confirmed via `javap` against the actual jars in this
environment: `OpenAiChatModel.Builder`, `AnthropicChatModel.Builder`,
`BaseGeminiChatModel.Builder`, and `OllamaBaseChatModel.Builder` in
langchain4j 1.19.0 all expose `httpClientBuilder(HttpClientBuilder)`; no
provider is special-cased — `ProxySelectorProvider` already bypasses
loopback/non-proxy-hosts correctly on its own, so applying this uniformly,
including to a typically-internal Ollama host, is safe and simpler than
special-casing):

```java
private HttpClientBuilder jdkHttpClientBuilder() {
    final HttpClient.Builder builder = HttpClient.newBuilder().proxy(proxySelectorProvider.get());
    proxyConfig.flatMap(ProxyConfig::credentials).ifPresent(credentials ->
            builder.authenticator(proxyOnlyAuthenticator(credentials.username(), credentials.password())));
    return new JdkHttpClientBuilder().httpClientBuilder(builder);
}
```

applied as `.httpClientBuilder(jdkHttpClientBuilder())` on each of
`openAi()`/`anthropic()`/`google()`/`ollama()`'s builder chains.
`proxyOnlyAuthenticator(String, String)` is a small private static helper
(new) building a `java.net.Authenticator` scoped to
`Authenticator.RequestorType.PROXY` only, so it's never offered for
non-proxy authentication challenges.

Bedrock (`bedrock()`) already builds its `BedrockRuntimeClient` via
`Apache5HttpClient.builder().socketTimeout(TIMEOUT).connectionTimeout(CONNECT_TIMEOUT)`.
New private helper (confirmed against the actual `apache5-client` jar in
this environment — `Apache5HttpClient.Builder#proxyConfiguration` and
`software.amazon.awssdk.http.apache5.ProxyConfiguration.Builder#endpoint`
both exist as expected):

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

applied in `bedrock()` by adding `bedrockProxyConfiguration().ifPresent(httpClientBuilder::proxyConfiguration)`
to the existing `Apache5HttpClient.builder()...` chain before it's passed to
`.httpClientBuilder(...)`. No interaction with `BedrockAuth.configureCredentials`
(operates on the separate `BedrockRuntimeClientBuilder`, not the HTTP client
builder) — confirmed by reading both call sites.

## Error handling

- `ProxyConfigConverter` wraps a malformed `http_proxy_uri` value's
  `URI.create()` `IllegalArgumentException` into JadConfig's
  `ParameterException`, matching `ProxyHostsPatternConverter`'s existing
  pattern — config validation fails clearly at startup instead of an
  unhandled `IllegalArgumentException` surfacing later, wherever the value
  first gets used.
- No other component introduces new error paths: every consumer's existing
  null/absence handling (`Optional`, `@Nullable`, `if (x != null)`) is
  preserved as-is, just re-pointed at `ProxyConfig` instead of `URI`.

## Testing

- **`ProxyConfigConverterTest`** (new, mirrors `ProxyHostsPatternConverterTest`):
  `convertFrom`/`convertTo` round-trip; a malformed value throws
  `ParameterException`.
- **`ProxyConfigTest`**: remove the two `.from(URI)` cases
  (`fromReturnsEmptyWithoutAConfiguredProxy`, `fromWrapsAConfiguredProxy`);
  add cases for `port(int, int)` — explicit port wins over both defaults;
  no port + http scheme → http default; no port + https scheme → https
  default.
- **`OkHttpClientProviderTest`**: the class-level and two method-level
  `@Disabled` annotations were already removed on a separate branch
  (`fix-okhttpclientprovider-test-flakiness`, off `master`, two commits:
  `6a9f2c4e3e`, `ed56c0bdb8`) for an unrelated live-DNS flakiness issue —
  merge/cherry-pick that fix into `proxy-config-record` first, since this
  work also touches this file's `client()` helper (constructs
  `ProxySelectorProvider`/`OkHttpClientProvider` with a `URI` today; needs
  a `ProxyConfig` after 2a/2b). Test call sites passing literal `null` for
  the proxy parameter need no change (`null` is assignable to either type);
  call sites constructing a real `URI.create(...)` need
  `new ProxyConfig(URI.create(...))` instead.
- **`AWSProxyConfigurationProviderTest`** (existing, 6 tests): all
  construct a `URI` and call the static `buildProxyConfiguration` directly
  — update every call site to `new ProxyConfig(URI.create(...))`; assertions
  on the returned `software.amazon.awssdk.http.apache.ProxyConfiguration`
  are unchanged (same host/port/username/password/-1-when-absent behavior,
  since `ProxyConfig.host()/port()/credentials()` already reproduce it
  exactly).
- **`AWSAsyncProxyConfigurationProviderTest`** (existing, 7 tests,
  including two specifically for the 80/443 default-port fallback): same
  update pattern; the two default-port tests are the direct regression
  signal for the new `ProxyConfig.port(int, int)` accessor.
- **`GitRepositoryServiceTest`**: constructs the service with a literal
  `null` for the proxy parameter — no change needed; confirmed no
  proxy-behavior-specific test exists to update.
- **`AWSInstanceNameLookupProcessorTest`**: none currently exists (confirm
  at implementation time this is genuinely untested, not just unfound by
  this spec's search). Treated as pure refactor, same as 2c-2f: the change
  is a getter swap with no behavior difference (`getHttpProxyConfig().isPresent()`
  is equivalent to the old `getHttpProxyUri() != null`, and
  `HttpUrl.get(proxyConfig.get().uri())` reconstructs the identical `HttpUrl`
  `HttpUrl.get(getHttpProxyUri())` did). No new test is added for this
  class as part of this change — it stays exactly as tested (i.e.
  untested) as it was before.
- **`ChatModelFactoryTest`** (existing, 2 tests, unrelated to proxies —
  `bedrockHandsBackTheClientToClose`, `theApiKeyProvidersHaveNothingToClose`):
  both construct `new ChatModelFactory()` — update to
  `new ChatModelFactory(new ProxySelectorProvider(null, null), null)` (no
  proxy configured), preserving their existing behavior. New tests added
  for the restored functionality: proxy applied/not-applied and
  credentials present/absent, for both the JDK-client path
  (`jdkHttpClientBuilder()`) and the Bedrock path
  (`bedrockProxyConfiguration()`) — six tests total, mirroring the shape
  the original (superseded) Task 5 draft specified, adapted to this
  design's actual method signatures. Any test needing a real target host
  uses a numeric/literal address, not a real hostname — learned directly
  from the `OkHttpClientProviderTest` flakiness root-cause in this same
  effort.

## Rollout / sequencing

Continues on the existing `proxy-config-record` branches (both repos —
they already carry the `ProxyConfig` foundation from Tasks 1-4). Order:

1. Merge/cherry-pick the `OkHttpClientProviderTest` flakiness fix into
   `proxy-config-record` (graylog2-server).
2. `ProxyConfigConverter` + `BaseConfiguration` change + `ProxyConfig.port(int,int)`
   + remove `ProxyConfig.from(URI)` (Component 1).
3. Revise `ProxySelectorProvider`, `OkHttpClientProvider` (2a, 2b) — these
   would fail to compile against step 2's `BaseConfiguration` change
   otherwise, since nothing would bind `@Named("http_proxy_uri") URI`
   anymore.
4. Migrate `AWSProxyConfigurationProvider`, `AWSAsyncProxyConfigurationProvider`,
   `GitRepositoryService`, `AWSInstanceNameLookupProcessor` (2c-2f) — same
   compile-dependency reasoning, can proceed in any order relative to each
   other once step 2 lands.
5. `ChatModelFactory` (2g) — depends on `graylog2-server`'s steps 2-4 being
   `mvn install`ed locally first, same cross-repo prerequisite the original
   plan already documented for its Task 5.

Steps 2-4 must land together as far as compilation is concerned (any
consumer left on the old `URI` injection breaks the moment
`BaseConfiguration`'s field type changes) — they can still be separate
commits/tasks for review granularity, just not separately mergeable to a
green build.
