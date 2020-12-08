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
const ContentPackUtils = {
  convertToString(parameter) {
    switch (parameter.type) {
      case 'integer':
      case 'double':
        return parameter.default_value.toString();
      case 'boolean':
        return parameter.default_value ? 'true' : 'false';
      default:
        return parameter.default_value;
    }
  },

  convertValue(type, value) {
    switch (type) {
      case 'integer':
        return parseInt(value, 10);
      case 'double':
        return parseFloat(value);
      case 'boolean':
        return value === 'true';
      default:
        return value;
    }
  },
};

export default ContentPackUtils;
