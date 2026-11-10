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
import { createGRN } from 'logic/permissions/GRN';
import useCurrentUser from 'hooks/useCurrentUser';
// eslint-disable-next-line no-restricted-imports
import { hasAdminPermission } from 'util/PermissionsMixin';
import type User from 'logic/users/User';

export const hasEntityOwnership = (currentUser: User | undefined, type: string, id: string | undefined) => {
  if (!currentUser) {
    return false;
  }

  const { grnPermissions = [], permissions } = currentUser;

  return hasAdminPermission(permissions) || grnPermissions.includes(`entity:own:${createGRN(type, id)}`);
};

const useHasEntityOwnership = (id: string | undefined, type: string) => {
  const currentUser = useCurrentUser();

  return hasEntityOwnership(currentUser, type, id);
};

export default useHasEntityOwnership;
