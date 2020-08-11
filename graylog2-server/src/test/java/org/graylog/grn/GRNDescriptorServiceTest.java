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
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GRNDescriptorServiceTest {
    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
    private final GRN user = grnRegistry.newGRN("user", "jane");
    private final GRN dashboard = grnRegistry.newGRN("dashboard", "abc123");

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
    void getDescriptors() {
        final ImmutableMap<GRNType, GRNDescriptorProvider> providers =  ImmutableMap.of(
                user.grnType(), grn -> GRNDescriptor.create(grn, "Jane Doe"),
                dashboard.grnType(), grn -> GRNDescriptor.create(grn, "A Dashboard")
        );
        final GRNDescriptorService service = new GRNDescriptorService(providers);

        final Set<GRNDescriptor> descriptors = service.getDescriptors(ImmutableSet.of(user, dashboard));

        assertThat(descriptors).containsExactlyInAnyOrder(
                GRNDescriptor.create(GRNTypes.USER.toGRN("jane"), "Jane Doe"),
                GRNDescriptor.create(GRNTypes.DASHBOARD.toGRN("abc123"), "A Dashboard")
        );
    }

    @Test
    void getDescriptorWithoutProvider() {
        final GRNDescriptorService service = new GRNDescriptorService(ImmutableMap.of());

        assertThatThrownBy(() -> service.getDescriptor(user))
                .hasMessageContaining(user.type())
                .isInstanceOf(IllegalStateException.class);
    }
}
