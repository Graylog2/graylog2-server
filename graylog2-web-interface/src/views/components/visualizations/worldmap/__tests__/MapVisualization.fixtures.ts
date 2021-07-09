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
const invalidData = [{
  keys: [{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}],
  name: 'TCP-count()',
  values: {
    '192.168.1.21': 64,
    '192.168.1.20': 16,
    '1.0.0.1': 45,
    '1.1.1.1': 41,
    '192.168.1.63': 20,
    '192.168.1.7': 8,
    '192.168.1.22': 5,
  },
}, {
  keys: [{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}],
  name: 'UDP-count()',
  values: {
    '192.168.1.21': 31,
    '192.168.1.1': 65,
    '192.168.1.20': 30,
    '192.168.1.7': 4,
    '8.8.8.8': 12,
    '192.168.1.22': 3,
  },
}, {
  keys: [{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}],
  name: 'count()',
  values: {
    '192.168.1.21': 95,
    '192.168.1.1': 65,
    '192.168.1.20': 46,
    '1.0.0.1': 45,
    '1.1.1.1': 41,
    '192.168.1.63': 20,
    '192.168.1.7': 12,
    '8.8.8.8': 12,
    '192.168.1.22': 8,
  },
}];

// eslint-disable-next-line import/prefer-default-export
export { invalidData };
