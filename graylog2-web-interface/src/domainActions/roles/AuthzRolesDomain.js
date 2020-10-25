// @flow strict
import type { ActionsType } from 'actions/roles/AuthzRolesActions';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import notifyingAction from '../notifyingAction';

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: AuthzRolesActions.load,
  error: (error, roleId) => ({
    message: `Loading role with id "${roleId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const deleteAction: $PropertyType<ActionsType, 'delete'> = notifyingAction({
  action: AuthzRolesActions.delete,
  success: (roleId, roleName) => ({
    message: `Role "${roleName}" was deleted successfully`,
  }),
  error: (error, roleId, roleName) => ({
    message: `Deleting role "${roleName}" failed with status: ${error}`,
  }),
});

const addMembers: $PropertyType<ActionsType, 'addMembers'> = notifyingAction({
  action: AuthzRolesActions.addMembers,
  success: (roleId, usernames) => ({
    message: `Users:"${usernames.join(', ')}" were assigned successfully`,
  }),
  error: (error, roleId, usernames) => ({
    message: `Assigning users "${usernames.join(', ')}" failed with status: ${error}`,
  }),
});

const removeMember: $PropertyType<ActionsType, 'removeMember'> = notifyingAction({
  action: AuthzRolesActions.removeMember,
  success: (roleId, username) => ({
    message: `User "${username}" was unassigned successfully`,
  }),
  error: (error, roleId, username) => ({
    message: `Unassign user "${username}" failed with status: ${error}`,
  }),
});

const loadUsersForRole: $PropertyType<ActionsType, 'loadUsersForRole'> = notifyingAction({
  action: AuthzRolesActions.loadUsersForRole,
  error: (error, username, roleName) => ({
    message: `Loading users for role "${roleName}" failed with status: ${error}`,
  }),
});

const loadRolesForUser: $PropertyType<ActionsType, 'loadRolesForUser'> = notifyingAction({
  action: AuthzRolesActions.loadRolesForUser,
  error: (error, username) => ({
    message: `Loading roles for user "${username}" failed with status: ${error}`,
  }),
});

const loadRolesPaginated: $PropertyType<ActionsType, 'loadRolesPaginated'> = notifyingAction({
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
