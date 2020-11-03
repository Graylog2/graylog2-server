// @flow strict
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
