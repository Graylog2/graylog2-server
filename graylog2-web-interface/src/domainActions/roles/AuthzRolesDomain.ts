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
import {
  loadRole,
  deleteRole,
  addMembersToRole,
  removeMemberFromRole,
  loadUsersForRole,
  loadRolesForUser,
  loadRolesPaginated,
} from 'hooks/useAuthzRoles';

import notifyingAction from '../notifyingAction';

const load = notifyingAction({
  action: loadRole,
  error: (error, roleId) => ({
    message: `Loading role with id "${roleId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const deleteAction = notifyingAction({
  action: (roleId: string, _roleName: string) => deleteRole(roleId),
  success: (_roleId, roleName) => ({
    message: `Role "${roleName}" was deleted successfully`,
  }),
  error: (error, _roleId, roleName) => ({
    message: `Deleting role "${roleName}" failed with status: ${error}`,
  }),
});

const addMembers = notifyingAction({
  action: addMembersToRole,
  success: (_roleId, usernames) => ({
    message: `Users:"${usernames.join(', ')}" were assigned successfully`,
  }),
  error: (error, _roleId, usernames) => ({
    message: `Assigning users "${usernames.join(', ')}" failed with status: ${error}`,
  }),
});

const removeMember = notifyingAction({
  action: removeMemberFromRole,
  success: (_roleId, username) => ({
    message: `User "${username}" was unassigned successfully`,
  }),
  error: (error, _roleId, username) => ({
    message: `Unassign user "${username}" failed with status: ${error}`,
  }),
});

const loadUsersForRoleAction = notifyingAction({
  action: (roleId: string, _roleName: string, pagination) => loadUsersForRole(roleId, pagination),
  error: (error, _roleId, roleName) => ({
    message: `Loading users for role "${roleName}" failed with status: ${error}`,
  }),
});

const loadRolesForUserAction = notifyingAction({
  action: loadRolesForUser,
  error: (error, username) => ({
    message: `Loading roles for user "${username}" failed with status: ${error}`,
  }),
});

const loadRolesPaginatedAction = notifyingAction({
  action: loadRolesPaginated,
  error: (error) => ({
    message: `Loading roles failed with status: ${error}`,
  }),
});

export default {
  load,
  delete: deleteAction,
  addMembers,
  removeMember,
  loadUsersForRole: loadUsersForRoleAction,
  loadRolesForUser: loadRolesForUserAction,
  loadRolesPaginated: loadRolesPaginatedAction,
};
