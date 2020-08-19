// @flow strict
import type { ActionsType } from 'actions/users/UsersActions';
import { UsersActions } from 'stores/users/UsersStore';

import notifyingAction from '../notifyingAction';

const create: $PropertyType<ActionsType, 'create'> = notifyingAction({
  action: UsersActions.create,
  success: (payload) => ({
    message: `User "${payload?.username}" was created successfully`,
  }),
  error: (error, payload) => ({
    message: `Updating user "${payload?.username}" failed with status: ${error}`,
  }),
});

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: UsersActions.load,
  error: (error, username) => ({
    message: `Loading user "${username}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const update: $PropertyType<ActionsType, 'update'> = notifyingAction({
  action: UsersActions.update,
  success: (username) => ({
    message: `User "${username}" was updated successfully`,
  }),
  error: (error, roleId, roleName) => ({
    message: `Updating user "${roleName}" failed with status: ${error}`,
  }),
});

const deleteAction: $PropertyType<ActionsType, 'delete'> = notifyingAction({
  action: UsersActions.delete,
  success: (username) => ({
    message: `User "${username}" was deleted successfully`,
  }),
  error: (error, username) => ({
    message: `Deleting user "${username}" failed with status: ${error}`,
  }),
});

const changePassword: $PropertyType<ActionsType, 'changePassword'> = notifyingAction({
  action: UsersActions.changePassword,
  success: (username) => ({
    message: `Password was changed successfully for user "${username}"`,
  }),
  error: (error, username) => ({
    message: `Changing password for user "${username}" failed with status: ${error}`,
  }),
});

const createToken: $PropertyType<ActionsType, 'createToken'> = notifyingAction({
  action: UsersActions.createToken,
  success: (username, tokenName) => ({
    message: `Token "${tokenName}" created successfully for user "${username}"`,
  }),
  error: (error, username, tokenName) => ({
    message: `Creating token "${tokenName}" for user "${username}" failed with status: ${error}`,
  }),
});

const loadTokens: $PropertyType<ActionsType, 'loadTokens'> = notifyingAction({
  action: UsersActions.loadTokens,
  error: (error, username) => ({
    message: `Loading token for user "${username}" failed with status: ${error}`,
  }),
});

const deleteToken: $PropertyType<ActionsType, 'deleteToken'> = notifyingAction({
  action: UsersActions.deleteToken,
  success: (username, tokenId, tokenName) => ({
    message: `Token "${tokenName}" deleted successfully for user "${username}"`,
  }),
  error: (error, username, tokenId, tokenName) => ({
    message: `Deleting token "${tokenName}" for user "${username}" failed with status: ${error}`,
  }),
});

const loadUsers: $PropertyType<ActionsType, 'loadUsers'> = notifyingAction({
  action: UsersActions.loadUsers,
  error: (error) => ({
    message: `Loading users failed with status: ${error}`,
  }),
});

const loadUsersPaginated: $PropertyType<ActionsType, 'loadUsersPaginated'> = notifyingAction({
  action: UsersActions.loadUsersPaginated,
  error: (error) => ({
    message: `Loading users failed with status: ${error}`,
  }),
});

export default {
  create,
  load,
  update,
  delete: deleteAction,
  changePassword,
  createToken,
  loadTokens,
  deleteToken,
  loadUsers,
  loadUsersPaginated,
};
