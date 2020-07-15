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

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GRNDescriptorServiceTest {
    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
    private final GRN user = grnRegistry.newGRN("user", "jane");

    @Test
    void getDescriptor() {
        final ImmutableMap<GRNType, GRNDescriptorProvider> providers =  ImmutableMap.of(
                user.grnType(), grn -> GRNDescriptor.create(grn, "Jane Doe")
        );
        final GRNDescriptorService service = new GRNDescriptorService(providers);

        assertThat(service.getDescriptor(user)).satisfies(descriptor -> {
            assertThat(descriptor.grn()).isEqualTo(user);
            assertThat(descriptor.title()).isEqualTo("Jane Doe");
        });
    }

    @Test
    void getDescriptorWithoutProvider() {
        final GRNDescriptorService service = new GRNDescriptorService(ImmutableMap.of());

        assertThatThrownBy(() -> service.getDescriptor(user))
                .hasMessageContaining(user.type())
                .isInstanceOf(IllegalStateException.class);
    }
}
