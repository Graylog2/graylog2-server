// @flow strict
import type { ActionsType } from 'actions/roles/AuthzRolesActions';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';

import notifyingAction from '../notifyingAction';

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: AuthzRolesActions.load,
  errorNotification: (error, roleId) => ({
    message: `Loading role with id "${roleId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const deleteAction: $PropertyType<ActionsType, 'delete'> = notifyingAction({
  action: AuthzRolesActions.delete,
  successNotification: (roleId, roleName) => ({
    message: `Role "${roleName}" was deleted successfully`,
  }),
  errorNotification: (error, roleId, roleName) => ({
    message: `Deleting role "${roleName}" failed with status: ${error}`,
  }),
});

const addMember: $PropertyType<ActionsType, 'addMember'> = notifyingAction({
  action: AuthzRolesActions.addMember,
  successNotification: (roleId, username) => ({
    message: `User "${username}" was assigned successfully`,
  }),
  errorNotification: (error, roleId, username) => ({
    message: `Assigning user "${username}" failed with status: ${error}`,
  }),
});

const removeMember: $PropertyType<ActionsType, 'removeMember'> = notifyingAction({
  action: AuthzRolesActions.removeMember,
  successNotification: (roleId, username) => ({
    message: `User "${username}" was unassigned successfully`,
  }),
  errorNotification: (error, roleId, username) => ({
    message: `Unassigning user "${username}" failed with status: ${error}`,
  }),
});

const loadUsersForRole: $PropertyType<ActionsType, 'loadUsersForRole'> = notifyingAction({
  action: AuthzRolesActions.loadUsersForRole,
  errorNotification: (error, username, roleName) => ({
    message: `Loading users for role "${roleName}" failed with status: ${error}`,
  }),
});

const loadRolesForUser: $PropertyType<ActionsType, 'loadRolesForUser'> = notifyingAction({
  action: AuthzRolesActions.loadRolesForUser,
  errorNotification: (error, username) => ({
    message: `Loading roles for user "${username}" failed with status: ${error}`,
  }),
});

const loadRolesPaginated: $PropertyType<ActionsType, 'loadRolesPaginated'> = notifyingAction({
  action: AuthzRolesActions.loadRolesPaginated,
  errorNotification: (error) => ({
    message: `Loading roles failed with status: ${error}`,
  }),
});

export default {
  load,
  delete: deleteAction,
  addMember,
  removeMember,
  loadUsersForRole,
  loadRolesForUser,
  loadRolesPaginated,
};
