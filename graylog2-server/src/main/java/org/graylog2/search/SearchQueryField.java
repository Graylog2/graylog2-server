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
package org.graylog2.search;

public class SearchQueryField {
    public enum Type {
        STRING, DATE;
    }

    private final String dbField;
    private final Type fieldType;

    public static SearchQueryField create(String dbField) {
        return new SearchQueryField(dbField, Type.STRING);
    }

    public static SearchQueryField create(String dbField, Type fieldType) {
        return new SearchQueryField(dbField, fieldType);
    }

    public SearchQueryField(String dbField, Type fieldType) {
        this.dbField = dbField;
        this.fieldType = fieldType;
    }

    public String getDbField() {
        return dbField;
    }

    public Type getFieldType() {
        return fieldType;
    }
}
