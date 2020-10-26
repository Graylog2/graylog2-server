// @flow strict
import type { ActionsType } from 'actions/users/UsersActions';
import { UsersActions } from 'stores/users/UsersStore';

import notifyingAction from '../notifyingAction';

const create: $PropertyType<ActionsType, 'create'> = notifyingAction({
  action: UsersActions.create,
  success: (user) => ({
    message: `User "${user?.full_name}" was created successfully`,
  }),
  error: (error, user) => ({
    message: `Creating user "${user?.full_name}" failed with status: ${error}`,
  }),
});

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: UsersActions.load,
  error: (error, userId) => ({
    message: `Loading user with id "${userId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const loadByUsername: $PropertyType<ActionsType, 'loadByUsername'> = notifyingAction({
  action: UsersActions.loadByUsername,
  error: (error, username) => ({
    message: `Loading user with username "${username}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const update: $PropertyType<ActionsType, 'update'> = notifyingAction({
  action: UsersActions.update,
  success: (userId, payload) => ({
    message: `User "${payload.full_name}" was updated successfully`,
  }),
  error: (error, userId, payload) => ({
    message: `Updating user "${payload.full_name}" failed with status: ${error}`,
  }),
});

const deleteAction: $PropertyType<ActionsType, 'delete'> = notifyingAction({
  action: UsersActions.delete,
  success: (username, fullName) => ({
    message: `User "${fullName}" was deleted successfully`,
  }),
  error: (error, username, fullName) => ({
    message: `Deleting user "${fullName}" failed with status: ${error}`,
  }),
});

const changePassword: $PropertyType<ActionsType, 'changePassword'> = notifyingAction({
  action: UsersActions.changePassword,
  success: () => ({
    message: 'Password was changed successfully ',
  }),
  error: (error, userId) => ({
    message: `Changing password for user with id "${userId}" failed with status: ${error}`,
  }),
});

const createToken: $PropertyType<ActionsType, 'createToken'> = notifyingAction({
  action: UsersActions.createToken,
  success: (userId, tokenName) => ({
    message: `Token "${tokenName}" created successfully`,
  }),
  error: (error, userId, tokenName) => ({
    message: `Creating token "${tokenName}" for user with id "${userId}" failed with status: ${error}`,
  }),
});

const loadTokens: $PropertyType<ActionsType, 'loadTokens'> = notifyingAction({
  action: UsersActions.loadTokens,
  error: (error, userId) => ({
    message: `Loading tokens for user with id "${userId}" failed with status: ${error}`,
  }),
});

const deleteToken: $PropertyType<ActionsType, 'deleteToken'> = notifyingAction({
  action: UsersActions.deleteToken,
  success: (userId, tokenId, tokenName) => ({
    message: `Token "${tokenName}" deleted successfully`,
  }),
  error: (error, userId, tokenId, tokenName) => ({
    message: `Deleting token "${tokenName}" for user with id "${userId}" failed with status: ${error}`,
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
  loadByUsername,
  update,
  delete: deleteAction,
  changePassword,
  createToken,
  loadTokens,
  deleteToken,
  loadUsers,
  loadUsersPaginated,
};
