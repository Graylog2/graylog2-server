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

import javax.inject.Inject;
import java.util.Map;

/**
 * Provides GRN descriptor instances.
 */
public class GRNDescriptorService {
    private final Map<GRNType, GRNDescriptorProvider> descriptorProviders;

    @Inject
    public GRNDescriptorService(Map<GRNType, GRNDescriptorProvider> descriptorProviders) {
        this.descriptorProviders = descriptorProviders;
    }

    /**
     * Returns the descriptor instance for the given GRN.
     *
     * @param grn the GRN
     * @return the descriptor instance for the GRN
     */
    public GRNDescriptor getDescriptor(GRN grn) {
        final GRNDescriptorProvider provider = descriptorProviders.get(grn.grnType());
        if (provider == null) {
            throw new IllegalStateException("Missing GRN descriptor provider for GRN type: " + grn.type());
        }
        return provider.get(grn);
    }
}
