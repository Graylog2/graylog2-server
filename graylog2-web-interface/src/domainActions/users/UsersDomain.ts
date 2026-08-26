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
import type { UserUpdate } from 'hooks/useUsers';
import {
  createUser,
  loadUser,
  loadUserByUsername,
  updateUser,
  deleteUser,
  changeUserPassword,
  createUserToken,
  loadUserTokens,
  deleteUserToken,
  loadUsers,
  loadUsersPaginated,
  setUserStatus,
} from 'hooks/useUsers';

import notifyingAction from '../notifyingAction';

const create = notifyingAction({
  action: createUser,
  success: (user) => ({
    message: `User "${user?.first_name} ${user?.last_name}" was created successfully`,
  }),
  error: (error, user) => ({
    message: `Creating user "${user?.first_name} ${user?.last_name}" failed with status: ${error}`,
  }),
});

const load = notifyingAction({
  action: loadUser,
  error: (error, userId) => ({
    message: `Loading user with id "${userId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const loadByUsername = notifyingAction({
  action: loadUserByUsername,
  error: (error, username) => ({
    message: `Loading user with username "${username}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const update = notifyingAction({
  action: (userId: string, request: UserUpdate, _fullName: string) => updateUser(userId, request),
  success: (_userId, _payload, fullName) => ({
    message: `User "${fullName}" was updated successfully`,
  }),
  error: (error, _userId, _payload, fullName) => ({
    message: `Updating user "${fullName}" failed with status: ${error}`,
  }),
});

const deleteAction = notifyingAction({
  action: (userId: string, _fullName: string) => deleteUser(userId),
  success: (_userId, fullName) => ({
    message: `User "${fullName}" was deleted successfully`,
  }),
  error: (error, _userId, fullName) => ({
    message: `Deleting user "${fullName}" failed with status: ${error}`,
  }),
});

const changePassword = notifyingAction({
  action: changeUserPassword,
  success: () => ({
    message: 'Password was changed successfully ',
  }),
  error: (error, userId) => ({
    message: `Changing password for user with id "${userId}" failed with status: ${error}`,
  }),
});

const createToken = notifyingAction({
  action: createUserToken,
  success: (_userId, tokenName) => ({
    message: `Token "${tokenName}" created successfully`,
  }),
  error: (error, userId, tokenName) => ({
    message: `Creating token "${tokenName}" for user with id "${userId}" failed with status: ${error}`,
  }),
});

const loadTokens = notifyingAction({
  action: loadUserTokens,
  error: (error, userId) => ({
    message: `Loading tokens for user with id "${userId}" failed with status: ${error}`,
  }),
});

const deleteToken = notifyingAction({
  action: (userId: string, tokenId: string, _tokenName: string) => deleteUserToken(userId, tokenId),
  success: (_userId, _tokenId, tokenName) => ({
    message: `Token "${tokenName}" deleted successfully`,
  }),
  error: (error, userId, _tokenId, tokenName) => ({
    message: `Deleting token "${tokenName}" for user with id "${userId}" failed with status: ${error}`,
  }),
});

const loadUsersAction = notifyingAction({
  action: loadUsers,
  error: (error) => ({
    message: `Loading users failed with status: ${error}`,
  }),
});

const loadUsersPaginatedAction = notifyingAction({
  action: loadUsersPaginated,
  error: (error) => ({
    message: `Loading users failed with status: ${error}`,
  }),
});

const setStatus = notifyingAction({
  action: setUserStatus,
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
  loadUsers: loadUsersAction,
  loadUsersPaginated: loadUsersPaginatedAction,
  setStatus,
};
