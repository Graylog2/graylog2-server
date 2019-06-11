package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.zafarkhaja.semver.Version;

import java.io.IOException;

public class VersionSerializer extends JsonSerializer<Version> {
    public VersionSerializer() {}

    @Override
    public void serialize(Version value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }
}
