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
package org.graylog.grn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GRNRegistryTest {
    private GRNType type;
    private GRNRegistry registry;

    @Nested
    @DisplayName("with entries")
    class WithBuiltins {
        @BeforeEach
        void setup() {
            type = GRNType.create("test", "tests:");
            registry = GRNRegistry.createWithTypes(Collections.singleton(type));
        }

        @Test
        @DisplayName("parse with existing type")
        void parseWithExistingType() {
            assertThat(registry.parse("grn::::test:123")).satisfies(grn -> {
                assertThat(grn.tenant()).isEmpty();
                assertThat(grn.cluster()).isEmpty();
                assertThat(grn.type()).isEqualTo("test");
                assertThat(grn.entity()).isEqualTo("123");
                assertThat(grn.grnType().permissionPrefix()).isEqualTo("tests:");
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
            assertThat(registry.newGRN(type, "123").toString()).isEqualTo("grn::::test:123");
        }

        @Test
        void newGRNBuilder() {
            assertThat(registry.newGRNBuilder("test").entity("123").build().toString()).isEqualTo("grn::::test:123");
            assertThat(registry.newGRNBuilder(type).entity("123").build().toString()).isEqualTo("grn::::test:123");
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
