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
package org.graylog2.indexer;

import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indexset.IndexSetConfig;

import javax.inject.Inject;
import java.util.Optional;

public class IndexSetValidator {
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public IndexSetValidator(IndexSetRegistry indexSetRegistry) {
        this.indexSetRegistry = indexSetRegistry;
    }

    public Optional<Violation> validate(IndexSetConfig newConfig) {
        // Build an example index name with the new prefix and check if this would be managed by an existing index set
        final String indexName = newConfig.indexPrefix() + MongoIndexSet.SEPARATOR + "0";
        if (indexSetRegistry.isManagedIndex(indexName)) {
            return Optional.of(Violation.create("Index prefix \"" + newConfig.indexPrefix() + "\" would conflict with an existing index set!"));
        }

        // Check if an existing index set has a more generic index prefix.
        // Example: new=graylog_foo existing=graylog => graylog is more generic so this is an error
        // Example: new=gray        existing=graylog => gray    is more generic so this is an error
        // This avoids problems with wildcard matching like "graylog_*".
        for (final IndexSet indexSet : indexSetRegistry) {
            if (newConfig.indexPrefix().startsWith(indexSet.getIndexPrefix()) || indexSet.getIndexPrefix().startsWith(newConfig.indexPrefix())) {
                return Optional.of(Violation.create("Index prefix \"" + newConfig.indexPrefix() + "\" would conflict with existing index set prefix \"" + indexSet.getIndexPrefix() + "\""));
            }
        }

        return Optional.empty();
    }

    @AutoValue
    public static abstract class Violation {
        public abstract String message();

        public static Violation create(String message) {
            return new AutoValue_IndexSetValidator_Violation(message);
        }
    }
}
