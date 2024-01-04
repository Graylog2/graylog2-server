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
export const profile1JSON = {
  custom_field_mappings: [{ field: 'http_method', type: 'string' }, { field: 'user_ip', type: 'ip' }],
  name: 'Profile 1',
  description: 'Description 1',
  id: '111',
};
export const profile2JSON = {
  custom_field_mappings: [{ field: 'user_name', type: 'string' }, { field: 'logged_in', type: 'boolean' }],
  name: 'Profile 2',
  description: 'Description 2',
  id: '222',
};

export const profile1 = {
  customFieldMappings: [{ field: 'http_method', type: 'string', id: 'http_method' }, { field: 'user_ip', type: 'ip', id: 'user_ip' }],
  name: 'Profile 1',
  description: 'Description 1',
  id: '111',
};
export const profile2 = {
  customFieldMappings: [{ field: 'user_name', type: 'string', id: 'user_name' }, { field: 'logged_in', type: 'boolean', id: 'logged_in' }],
  name: 'Profile 2',
  description: 'Description 2',
  id: '222',
};

export const profile1FormValues = {
  customFieldMappings: [{ field: 'http_method', type: 'string' }, { field: 'user_ip', type: 'ip' }],
  name: 'Profile 1',
  description: 'Description 1',
  id: '111',
};
export const profile2FormValues = {
  customFieldMappings: [{ field: 'user_name', type: 'string' }, { field: 'logged_in', type: 'boolean' }],
  name: 'Profile 2',
  description: 'Description 2',
  id: '222',
};
