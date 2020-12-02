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
import { $PropertyType } from 'utility-types';

import User from 'logic/users/User';

// eslint-disable-next-line import/prefer-default-export
export const readerPermissions = (username: $PropertyType<User, 'username'>) => [
  `users:tokenlist:${username}`,
  `users:edit:${username}`,
  `users:tokencreate:${username}`,
  `users:tokenremove:${username}`,
  `users:passwordchange:${username}`,
  'metrics:read',
  'messagecount:read',
  'journal:read',
  'messages:analyze',
  'fieldnames:read',
  'messages:read',
  'indexercluster:read',
  'system:read',
  'jvmstats:read',
  'inputs:read',
  'buffers:read',
  'clusterconfigentry:read',
  'decorators:read',
  'throughput:read',
];
