package org.graylog2.utilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GRNRegistryTest {
    private GRNRegistry registry;

    @Nested
    @DisplayName("with entries")
    class WithBuiltins {
        @BeforeEach
        void setup() {
            registry = GRNRegistry.createWithTypes(Collections.singleton(GRNType.create("test", "tests:")));
        }

        @Test
        @DisplayName("parse with existing type")
        void parseWithExistingType() {
            assertThat(registry.parse("grn::::test:123")).satisfies(grn -> {
                assertThat(grn.tenant()).isEmpty();
                assertThat(grn.cluster()).isEmpty();
                assertThat(grn.type()).isEqualTo("test");
                assertThat(grn.entity()).isEqualTo("123");
                assertThat(grn.permissionPrefix()).isEqualTo("tests:");
            });
        }

        @Test
        @DisplayName("parse with missing type should fail")
        void parseWithMissingType() {
            assertThatThrownBy(() -> registry.parse("grn::::__foo__:123"))
                    .hasMessageContaining("__foo__")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void newGRN() {
            assertThat(registry.newGRN("test", "123").toString()).isEqualTo("grn::::test:123");
        }
    }

    @Nested
    @DisplayName("when empty")
    class WhenEmpty {
        @BeforeEach
        void setup() {
            registry = GRNRegistry.createEmpty();
        }

        @Test
        @DisplayName("parse should fail")
        void parse() {
            assertThatThrownBy(() -> registry.parse("grn::::collection:123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("newGRNBuilder should fail")
        void newGRNBuilder() {
            assertThatThrownBy(() -> registry.newGRNBuilder("collection"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}