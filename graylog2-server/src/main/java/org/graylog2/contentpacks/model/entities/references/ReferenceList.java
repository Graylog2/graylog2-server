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
package org.graylog2.contentpacks.model.entities.references;

import com.google.common.collect.ForwardingList;

import java.util.ArrayList;
import java.util.List;

public class ReferenceList extends ForwardingList<Reference> implements Reference {
    private final List<Reference> list;

    public ReferenceList(List<Reference> list) {
        this.list = list;
    }

    public ReferenceList() {
        this(new ArrayList<>());
    }

    @Override
    protected List<Reference> delegate() {
        return list;
    }
}
