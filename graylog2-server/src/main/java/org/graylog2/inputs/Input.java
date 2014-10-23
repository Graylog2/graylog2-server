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
package org.graylog2.inputs;

import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;

import java.util.Map;

public interface Input extends Persisted {
    String getTitle();

    DateTime getCreatedAt();

    Map<String, Object> getConfiguration();

    Map<String, String> getStaticFields();

    String getType();

    String getCreatorUserId();

    String getInputId();

    Boolean isGlobal();

    String getContentPack();
}
