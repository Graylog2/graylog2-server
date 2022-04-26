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
import { UsersActions } from 'stores/users/UsersStore';

import notifyingAction from '../notifyingAction';

const create = notifyingAction({
  action: UsersActions.create,
  success: (user) => ({
    message: `User "${user?.first_name} ${user?.last_name}" was created successfully`,
  }),
  error: (error, user) => ({
    message: `Creating user "${user?.first_name} ${user?.last_name}" failed with status: ${error}`,
  }),
});

const load = notifyingAction({
  action: UsersActions.load,
  error: (error, userId) => ({
    message: `Loading user with id "${userId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const loadByUsername = notifyingAction({
  action: UsersActions.loadByUsername,
  error: (error, username) => ({
    message: `Loading user with username "${username}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const update = notifyingAction({
  action: UsersActions.update,
  success: (_userId, _payload, fullName) => ({
    message: `User "${fullName}" was updated successfully`,
  }),
  error: (error, _userId, _payload, fullName) => ({
    message: `Updating user "${fullName}" failed with status: ${error}`,
  }),
});

const deleteAction = notifyingAction({
  action: UsersActions.delete,
  success: (_userId, fullName) => ({
    message: `User "${fullName}" was deleted successfully`,
  }),
  error: (error, _userId, fullName) => ({
    message: `Deleting user "${fullName}" failed with status: ${error}`,
  }),
});

const changePassword = notifyingAction({
  action: UsersActions.changePassword,
  success: () => ({
    message: 'Password was changed successfully ',
  }),
  error: (error, userId) => ({
    message: `Changing password for user with id "${userId}" failed with status: ${error}`,
  }),
});

const createToken = notifyingAction({
  action: UsersActions.createToken,
  success: (_userId, tokenName) => ({
    message: `Token "${tokenName}" created successfully`,
  }),
  error: (error, userId, tokenName) => ({
    message: `Creating token "${tokenName}" for user with id "${userId}" failed with status: ${error}`,
  }),
});

const loadTokens = notifyingAction({
  action: UsersActions.loadTokens,
  error: (error, userId) => ({
    message: `Loading tokens for user with id "${userId}" failed with status: ${error}`,
  }),
});

const deleteToken = notifyingAction({
  action: UsersActions.deleteToken,
  success: (_userId, _tokenId, tokenName) => ({
    message: `Token "${tokenName}" deleted successfully`,
  }),
  error: (error, userId, _tokenId, tokenName) => ({
    message: `Deleting token "${tokenName}" for user with id "${userId}" failed with status: ${error}`,
  }),
});

const loadUsers = notifyingAction({
  action: UsersActions.loadUsers,
  error: (error) => ({
    message: `Loading users failed with status: ${error}`,
  }),
});

const loadUsersPaginated = notifyingAction({
  action: UsersActions.loadUsersPaginated,
  error: (error) => ({
    message: `Loading users failed with status: ${error}`,
  }),
});

const setStatus = notifyingAction({
  action: UsersActions.setStatus,
  success: (userId, accountStatus) => ({
    message: `User "${userId}" was set to ${accountStatus}`,
  }),
  error: (error, userId, accountStatus) => ({
    message: `Updating user ("${userId}") to ${accountStatus} failed with status: ${error}`,
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
  setStatus,
};
