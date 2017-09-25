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
package org.graylog2.lookup.adapters;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.floreysoft.jmte.Engine;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.plugin.lookup.LookupResult;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

public class HTTPJSONPathDataAdapter extends LookupDataAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(HTTPJSONPathDataAdapter.class);
    public static final String NAME = "httpjsonpath";

    private final Config config;
    private final Engine templateEngine;
    private final OkHttpClient httpClient;

    private final Timer httpRequestTimer;
    private final Meter httpRequestErrors;
    private final Meter httpURLErrors;

    private JsonPath singleJsonPath = null;
    private JsonPath multiJsonPath = null;
    private Headers headers;

    @Inject
    protected HTTPJSONPathDataAdapter(@Assisted LookupDataAdapterConfiguration config,
                                      @Assisted("id") String id,
                                      @Assisted("name") String name,
                                      Engine templateEngine,
                                      OkHttpClient httpClient,
                                      MetricRegistry metricRegistry) {
        super(id, name, config, metricRegistry);
        this.config = (Config) config;
        this.templateEngine = templateEngine;
        // TODO Add config options: caching, timeouts, custom headers, basic auth (See: https://github.com/square/okhttp/wiki/Recipes)
        this.httpClient = httpClient.newBuilder().build(); // Copy HTTP client to be able to modify it

        this.httpRequestTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "httpRequestTime"));
        this.httpRequestErrors = metricRegistry.meter(MetricRegistry.name(getClass(), "httpRequestErrors"));
        this.httpURLErrors = metricRegistry.meter(MetricRegistry.name(getClass(), "httpURLErrors"));
    }

    @Override
    protected void doStart() throws Exception {
        if (isNullOrEmpty(config.url())) {
            throw new IllegalArgumentException("URL needs to be set");
        }
        if (isNullOrEmpty(config.singleValueJSONPath())) {
            throw new IllegalArgumentException("Value JSONPath needs to be set");
        }

        this.singleJsonPath = JsonPath.compile(config.singleValueJSONPath());

        // The JSONPath for the single value cannot be indefinite. (https://github.com/json-path/JsonPath#what-is-returned-when)
        if (!singleJsonPath.isDefinite()) {
            throw new IllegalArgumentException("Single JSONPath <" + config.singleValueJSONPath() + "> cannot return a list");
        }

        if (config.multiValueJSONPath().isPresent() && !isNullOrEmpty(config.multiValueJSONPath().get())) {
            this.multiJsonPath = JsonPath.compile(config.multiValueJSONPath().get());
        }

        final Headers.Builder headersBuilder = new Headers.Builder()
                .add(HttpHeaders.USER_AGENT, config.userAgent())
                .add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        if (config.headers() != null) {
            config.headers().forEach(headersBuilder::set);
        }
        this.headers = headersBuilder.build();
    }

    @Override
    protected void doStop() throws Exception {
    }

    @Override
    public Duration refreshInterval() {
        return Duration.ZERO;
    }

    @Override
    protected void doRefresh(LookupCachePurge cachePurge) throws Exception {
    }

    @Override
    protected LookupResult doGet(Object key) {
        String encodedKey;
        try {
            encodedKey = URLEncoder.encode(String.valueOf(key), "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException ignored) {
            // UTF-8 is always supported
            encodedKey = String.valueOf(key);
        }
        final String urlString = templateEngine.transform(config.url(), ImmutableMap.of("key", encodedKey));
        final HttpUrl url = HttpUrl.parse(urlString);

        if (url == null) {
            LOG.error("Couldn't parse URL <%s> - returning empty result", urlString);
            httpURLErrors.mark();
            return LookupResult.empty();
        }

        final Request request = new Request.Builder()
                .get()
                .url(url)
                .headers(headers)
                .build();

        final Timer.Context time = httpRequestTimer.time();
        try (final Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOG.warn("HTTP request for key <{}> failed: {}", key, response);
                httpRequestErrors.mark();
                return LookupResult.empty();
            }

            return parseBody(singleJsonPath, multiJsonPath, response.body().byteStream());
        } catch (IOException e) {
            LOG.error("HTTP request error for key <{}>", key, e);
            httpRequestErrors.mark();
            return LookupResult.empty();
        } finally {
            time.stop();
        }
    }

    @VisibleForTesting
    static LookupResult parseBody(JsonPath singleJsonPath, @Nullable JsonPath multiJsonPath, InputStream body) {
        try {
            final DocumentContext documentContext = JsonPath.parse(body);

            LookupResult.Builder builder = LookupResult.builder().cacheTTL(Long.MAX_VALUE);

            if (multiJsonPath != null) {
                try {
                    final Object multiValue = documentContext.read(multiJsonPath);

                    if (multiValue instanceof Map) {
                        //noinspection unchecked
                        builder = builder.multiValue((Map<Object, Object>) multiValue);
                    } else {
                        builder = builder.multiSingleton(multiValue);
                    }
                } catch (PathNotFoundException e) {
                    LOG.warn("Couldn't read multi JSONPath from response - skipping multi value ({})", e.getMessage());
                }
            }

            try {
                final Object singleValue = documentContext.read(singleJsonPath);

                if (singleValue instanceof CharSequence) {
                    return builder.single((CharSequence) singleValue).build();
                } else if (singleValue instanceof Number) {
                    return builder.single((Number) singleValue).build();
                } else if (singleValue instanceof Boolean) {
                    return builder.single((Boolean) singleValue).build();
                } else {
                    throw new IllegalArgumentException("Single value data type cannot be: " + singleValue.getClass().getCanonicalName());
                }
            } catch (PathNotFoundException e) {
                LOG.warn("Couldn't read single JSONPath from response - returning empty result ({})", e.getMessage());
                return LookupResult.empty();
            }
        } catch (InvalidJsonException e) {
            LOG.error("Couldn't parse JSON response", e);
            return LookupResult.empty();
        } catch (ClassCastException e) {
            LOG.error("Couldn't assign value type", e);
            return LookupResult.empty();
        } catch (Exception e) {
            LOG.error("Unexpected error parsing JSON response", e);
            return LookupResult.empty();
        }
    }

    @Override
    public void set(Object key, Object value) {
    }

    public interface Factory extends LookupDataAdapter.Factory<HTTPJSONPathDataAdapter> {
        @Override
        HTTPJSONPathDataAdapter create(@Assisted("id") String id,
                                       @Assisted("name") String name,
                                       LookupDataAdapterConfiguration configuration);

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends LookupDataAdapter.Descriptor<Config> {
        public Descriptor() {
            super(NAME, Config.class);
        }

        @Override
        public Config defaultConfiguration() {
            return Config.builder()
                    .type(NAME)
                    .url("")
                    .singleValueJSONPath("$.value")
                    .userAgent("Graylog Lookup - https://www.graylog.org/")
                    .headers(Collections.emptyMap())
                    .build();
        }
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    @JsonDeserialize(builder = AutoValue_HTTPJSONPathDataAdapter_Config.Builder.class)
    @JsonTypeName(NAME)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static abstract class Config implements LookupDataAdapterConfiguration {
        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty("url")
        @NotEmpty
        public abstract String url();

        @JsonProperty("single_value_jsonpath")
        @NotEmpty
        public abstract String singleValueJSONPath();

        @JsonProperty("multi_value_jsonpath")
        public abstract Optional<String> multiValueJSONPath();

        @JsonProperty("user_agent")
        @NotEmpty
        public abstract String userAgent();

        @JsonProperty("headers")
        @Nullable
        public abstract Map<String, String> headers();

        public static Builder builder() {
            return new AutoValue_HTTPJSONPathDataAdapter_Config.Builder();
        }

        @Override
        public Optional<Multimap<String, String>> validate() {
            final ArrayListMultimap<String, String> errors = ArrayListMultimap.create();

            if (HttpUrl.parse(url()) == null) {
                errors.put("url", "Invalid URL.");
            }

            try {
                final JsonPath jsonPath = JsonPath.compile(singleValueJSONPath());
                if (!jsonPath.isDefinite()) {
                    errors.put("single_value_jsonpath", "JSONPath does not return a single value.");
                }
            } catch (InvalidPathException e) {
                errors.put("single_value_jsonpath", "Invalid JSONPath.");
            }
            if (multiValueJSONPath().isPresent()) {
                try {
                    JsonPath.compile(multiValueJSONPath().get());
                } catch (InvalidPathException e) {
                    errors.put("multi_value_jsonpath", "Invalid JSONPath.");
                }
            }

            return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty("url")
            public abstract Builder url(String url);

            @JsonProperty("single_value_jsonpath")
            public abstract Builder singleValueJSONPath(String singleValueJSONPath);

            @JsonProperty("multi_value_jsonpath")
            public abstract Builder multiValueJSONPath(String multiValueJSONPath);

            @JsonProperty("user_agent")
            public abstract Builder userAgent(String userAgent);

            @JsonProperty("headers")
            public abstract Builder headers(Map<String, String> headers);

            public abstract Config build();
        }
    }
}
