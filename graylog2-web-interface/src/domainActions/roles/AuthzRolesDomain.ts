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
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import notifyingAction from '../notifyingAction';

const load = notifyingAction({
  action: AuthzRolesActions.load,
  error: (error, roleId) => ({
    message: `Loading role with id "${roleId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const deleteAction = notifyingAction({
  action: AuthzRolesActions.delete,
  success: (_roleId, roleName) => ({
    message: `Role "${roleName}" was deleted successfully`,
  }),
  error: (error, _roleId, roleName) => ({
    message: `Deleting role "${roleName}" failed with status: ${error}`,
  }),
});

const addMembers = notifyingAction({
  action: AuthzRolesActions.addMembers,
  success: (_roleId, usernames) => ({
    message: `Users:"${usernames.join(', ')}" were assigned successfully`,
  }),
  error: (error, _roleId, usernames) => ({
    message: `Assigning users "${usernames.join(', ')}" failed with status: ${error}`,
  }),
});

const removeMember = notifyingAction({
  action: AuthzRolesActions.removeMember,
  success: (_roleId, username) => ({
    message: `User "${username}" was unassigned successfully`,
  }),
  error: (error, _roleId, username) => ({
    message: `Unassign user "${username}" failed with status: ${error}`,
  }),
});

const loadUsersForRole = notifyingAction({
  action: AuthzRolesActions.loadUsersForRole,
  error: (error, _roleId, roleName) => ({
    message: `Loading users for role "${roleName}" failed with status: ${error}`,
  }),
});

const loadRolesForUser = notifyingAction({
  action: AuthzRolesActions.loadRolesForUser,
  error: (error, username) => ({
    message: `Loading roles for user "${username}" failed with status: ${error}`,
  }),
});

const loadRolesPaginated = notifyingAction({
  action: AuthzRolesActions.loadRolesPaginated,
  error: (error) => ({
    message: `Loading roles failed with status: ${error}`,
  }),
});

export default {
  load,
  delete: deleteAction,
  addMembers,
  removeMember,
  loadUsersForRole,
  loadRolesForUser,
  loadRolesPaginated,
};
