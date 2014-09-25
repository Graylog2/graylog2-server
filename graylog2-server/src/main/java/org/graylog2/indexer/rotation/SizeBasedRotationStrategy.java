/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.rotation;

import com.google.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.text.MessageFormat;

@Singleton
public class SizeBasedRotationStrategy implements RotationStrategy {

    private final Indices indices;
    private final long maxSize;

    @Inject
    public SizeBasedRotationStrategy(Configuration configuration, Indices indices) {
        this.indices = indices;
        maxSize = configuration.getElasticSearchMaxSizePerIndex();
    }

    @Nullable
    @Override
    public Result shouldRotate(final String index) {

        final IndexStatistics indexStats = indices.getIndexStats(index);
        if (indexStats == null) {
            return null;
        }

        final long sizeInBytes = indexStats.getPrimaries().store.getSizeInBytes();

        final boolean shouldRotate = sizeInBytes > maxSize;

        return new Result() {
            public final MessageFormat ROTATE = new MessageFormat("Storage size for index <{0}> is {1} bytes, exceeding the maximum of {2} bytes. Rotating index.");
            public final MessageFormat NOT_ROTATE = new MessageFormat("Storage size for index <{0}> is {1} bytes, below the maximum of {2} bytes. Not doing anything.");

            @Override
            public String getDescription() {
                MessageFormat format = shouldRotate() ? ROTATE : NOT_ROTATE;
                return format.format(new Object[] {
                        index,
                        sizeInBytes,
                        maxSize
                });
            }

            @Override
            public boolean shouldRotate() {
                return shouldRotate;
            }
        };
    }
}
