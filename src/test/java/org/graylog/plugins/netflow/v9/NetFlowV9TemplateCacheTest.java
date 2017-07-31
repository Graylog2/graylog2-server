package org.graylog.plugins.netflow.v9;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

public class NetFlowV9TemplateCacheTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private Path cachePath;
    private ScheduledExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newSingleThreadScheduledExecutor();
        cachePath = temporaryFolder.newFile().toPath();
    }

    @Test
    public void getReturnsCorrectValue() throws Exception {
        final NetFlowV9TemplateCache templateCache = new NetFlowV9TemplateCache(100L, cachePath, 300, executorService, objectMapper);

        final NetFlowV9Template template = NetFlowV9Template.create(0, 0, Collections.emptyList());
        templateCache.put(template);

        assertThat(templateCache.get(0)).isEqualTo(template);
    }

    @Test
    public void putOverwritesValue() throws Exception {
        final NetFlowV9TemplateCache templateCache = new NetFlowV9TemplateCache(100L, cachePath, 300, executorService, objectMapper);

        final NetFlowV9Template template1 = NetFlowV9Template.create(0, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.UINT32, "foobar"), 4)));
        final NetFlowV9Template template2 = NetFlowV9Template.create(0, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.UINT32, "covfefe"), 4)));
        templateCache.put(template1);

        assertThat(templateCache.get(0)).isEqualTo(template1);
        templateCache.put(template2);
        assertThat(templateCache.get(0)).isEqualTo(template2);
    }

    @Test
    public void runSavesCacheInGivenInterval() throws Exception {
        final int saveInterval = 1;
        final NetFlowV9TemplateCache templateCache = new NetFlowV9TemplateCache(100L, cachePath, saveInterval, executorService, objectMapper);
        assertThat(Files.size(cachePath)).isEqualTo(0L);

        final NetFlowV9Template template1 = NetFlowV9Template.create(0, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.UINT64, "foobar"), 8)));
        final byte[] expectedJson1 = ("{\"0\":{\"template_id\":0,\"field_count\":1,\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"UINT64\",\"name\":\"foobar\"},\"length\":8}]}}").getBytes(StandardCharsets.UTF_8);
        final NetFlowV9Template template2 = NetFlowV9Template.create(1, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.IPV4, "covfefe"), 4)));
        final byte[] expectedJson2 = ("{" +
                "\"0\":{" +
                "\"template_id\":0," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"UINT64\",\"name\":\"foobar\"},\"length\":8}]" +
                "}," +
                "\"1\":{" +
                "\"template_id\":1," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"IPV4\",\"name\":\"covfefe\"},\"length\":4}]}}")
                .getBytes(StandardCharsets.UTF_8);

        // Sleep for initial delay
        Thread.sleep(6000L);

        templateCache.put(template1);
        await().atMost(saveInterval * 2, TimeUnit.SECONDS)
                .until(() -> Arrays.equals(Files.readAllBytes(cachePath), expectedJson1));

        templateCache.put(template2);
        await().atMost(saveInterval * 2, TimeUnit.SECONDS)
                .until(() -> Arrays.equals(Files.readAllBytes(cachePath), expectedJson2));
    }

    @Test
    public void runSavesCacheToExistingFile() throws Exception {
        final NetFlowV9TemplateCache templateCache = new NetFlowV9TemplateCache(100L, cachePath, 300, executorService, objectMapper);

        final NetFlowV9Template template1 = NetFlowV9Template.create(0, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.UINT64, "foobar"), 8)));
        final NetFlowV9Template template2 = NetFlowV9Template.create(1, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.IPV4, "covfefe"), 4)));
        templateCache.put(template1);
        templateCache.put(template2);

        assertThat(Files.exists(cachePath)).isTrue();
        assertThat(Files.size(cachePath)).isEqualTo(0L);

        templateCache.run();

        assertThat(Files.exists(cachePath)).isTrue();
        assertThat(Files.size(cachePath)).isGreaterThan(0L);

        final byte[] expectedJson = ("{" +
                "\"0\":{" +
                "\"template_id\":0," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"UINT64\",\"name\":\"foobar\"},\"length\":8}]" +
                "}," +
                "\"1\":{" +
                "\"template_id\":1," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"IPV4\",\"name\":\"covfefe\"},\"length\":4}]}}")
                .getBytes(StandardCharsets.UTF_8);
        assertThat(Files.readAllBytes(cachePath)).isEqualTo(expectedJson);
    }

    @Test
    public void runSavesCacheToNonExistingFile() throws Exception {
        assertThat(cachePath.toFile().delete()).isTrue();
        assertThat(cachePath.toFile().exists()).isFalse();

        final NetFlowV9TemplateCache templateCache = new NetFlowV9TemplateCache(100L, cachePath, 300, executorService, objectMapper);

        final NetFlowV9Template template1 = NetFlowV9Template.create(0, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.UINT64, "foobar"), 8)));
        final NetFlowV9Template template2 = NetFlowV9Template.create(1, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.IPV4, "covfefe"), 4)));
        templateCache.put(template1);
        templateCache.put(template2);

        assertThat(Files.exists(cachePath)).isFalse();

        templateCache.run();

        assertThat(Files.exists(cachePath)).isTrue();
        assertThat(Files.size(cachePath)).isGreaterThan(0L);

        final byte[] expectedJson = ("{" +
                "\"0\":{" +
                "\"template_id\":0," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"UINT64\",\"name\":\"foobar\"},\"length\":8}]" +
                "}," +
                "\"1\":{" +
                "\"template_id\":1," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"IPV4\",\"name\":\"covfefe\"},\"length\":4}]}}")
                .getBytes(StandardCharsets.UTF_8);
        assertThat(Files.readAllBytes(cachePath)).isEqualTo(expectedJson);
    }

    @Test
    public void loadsCacheFileOnStart() throws Exception {
        final byte[] json = ("{" +
                "\"0\":{" +
                "\"template_id\":0," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"UINT64\",\"name\":\"foobar\"},\"length\":8}]" +
                "}," +
                "\"1\":{" +
                "\"template_id\":1," +
                "\"field_count\":1," +
                "\"definitions\":[{\"type\":{\"id\":0,\"value_type\":\"IPV4\",\"name\":\"covfefe\"},\"length\":4}]}}")
                .getBytes(StandardCharsets.UTF_8);
        assertThat(Files.write(cachePath, json)).isEqualTo(cachePath);
        assertThat(Files.size(cachePath)).isEqualTo(json.length);

        final NetFlowV9TemplateCache templateCache = new NetFlowV9TemplateCache(100L, cachePath, 300, executorService, objectMapper);

        final NetFlowV9Template template1 = NetFlowV9Template.create(0, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.UINT64, "foobar"), 8)));
        final NetFlowV9Template template2 = NetFlowV9Template.create(1, 1, ImmutableList.of(NetFlowV9FieldDef.create(NetFlowV9FieldType.create(0, NetFlowV9FieldType.ValueType.IPV4, "covfefe"), 4)));

        assertThat(templateCache.get(0)).isEqualTo(template1);
        assertThat(templateCache.get(1)).isEqualTo(template2);
    }
}