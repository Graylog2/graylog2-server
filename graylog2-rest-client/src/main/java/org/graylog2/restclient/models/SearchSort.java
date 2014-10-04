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
package org.graylog2.restclient.models;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SearchSort {

    public enum Direction {
        ASC,
        DESC
    }

    private final String field;
    private final Direction direction;

    public SearchSort(String field, Direction direction) {
        this.field = field;
        this.direction = direction;
    }

    public boolean isAscending() {
        return direction.equals(Direction.ASC);
    }

    public boolean isDescending() {
        return direction.equals(Direction.DESC);
    }

    public String getField() {
        return field;
    }

    public Direction getDirection() {
        return direction;
    }

    public String toApiParam() {
        StringBuilder sb = new StringBuilder();

        sb.append(field).append(":").append(direction.toString().toLowerCase());

        return sb.toString();
    }

}
