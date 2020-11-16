/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
const DecorationStats = {
  isFieldAddedByDecorator(message, fieldName) {
    const decorationStats = message.decoration_stats;

    return decorationStats && decorationStats.added_fields && decorationStats.added_fields[fieldName] !== undefined;
  },

  isFieldChangedByDecorator(message, fieldName) {
    const decorationStats = message.decoration_stats;

    return decorationStats && decorationStats.changed_fields && decorationStats.changed_fields[fieldName] !== undefined;
  },

  isFieldDecorated(message, fieldName) {
    return this.isFieldAddedByDecorator(message, fieldName) || this.isFieldChangedByDecorator(message, fieldName);
  },
};

export default DecorationStats;
