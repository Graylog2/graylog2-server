package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.zafarkhaja.semver.Version;
import org.graylog2.storage.versionprobe.SearchVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MajorVersionConverterTest {

    private static final Converter<SearchVersion> converter = new MajorVersionConverter();

    @Test
    void convertSimpleNumber() {
        final SearchVersion version = converter.convertFrom("7");
        assertThat(version).isEqualTo(SearchVersion.elasticsearch("7.0.0"));
    }

    @Test
    void convertEncodedValue() {
        final SearchVersion version = converter.convertFrom("OPENSEARCH:1.2.0");
        assertThat(version).isEqualTo(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.2.0")));
    }

    @Test
    void testConvertToString() {
        final String converted = converter.convertTo(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.2.0")));
        assertThat(converted).isEqualTo("OPENSEARCH:1.2.0");
    }
}
