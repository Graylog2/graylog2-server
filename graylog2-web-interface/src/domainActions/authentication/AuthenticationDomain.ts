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
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';

import notifyingAction from '../notifyingAction';

const create = notifyingAction({
  action: AuthenticationActions.create,
  success: (authBackend) => ({
    message: `Authentication service "${authBackend.title} was created successfully`,
  }),
  error: (error, authBackend) => ({
    message: `Creating authentication service "${authBackend.title}" failed with status: ${error}`,
  }),
});

const load = notifyingAction({
  action: AuthenticationActions.load,
  error: (error, authBackendId) => ({
    message: `Loading authentication service with id "${authBackendId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const loadActive = notifyingAction({
  action: AuthenticationActions.loadActive,
  error: (error) => ({
    message: `Loading active authentication service failed with status: ${error}`,
  }),
});

const update = notifyingAction({
  action: AuthenticationActions.update,
  success: (_authBackendId, authBackend) => ({
    message: `Authentication service "${authBackend.title}" was updated successfully`,
  }),
  error: (error, _authBackendId, authBackend) => ({
    message: `Updating authentication service "${authBackend.title}" failed with status: ${error}`,
  }),
});

const deleteBackend = notifyingAction({
  action: AuthenticationActions.delete,
  success: (_authBackendId, authBackendTitle) => ({
    message: `Authentication service "${authBackendTitle} was deleted successfully`,
  }),
  error: (error, _authBackendId, authBackendTitle) => ({
    message: `Deleting authentication service "${authBackendTitle}" failed with status: ${error}`,
  }),
});

const testConnection = notifyingAction({
  action: AuthenticationActions.testConnection,
  error: (error) => ({
    message: `Connection test failed with status: ${error}`,
  }),
});

const testLogin = notifyingAction({
  action: AuthenticationActions.testLogin,
  error: (error) => ({
    message: `Login test failed with status: ${error}`,
  }),
});

const setActiveBackend = notifyingAction({
  action: AuthenticationActions.setActiveBackend,
  success: (authBackendId, authBackendTitle) => ({
    message: `Authentication service "${authBackendTitle} was ${authBackendId ? 'activated' : 'deactivated'} successfully`,
  }),
  error: (error, _authBackendId, authBackendTitle) => ({
    message: `Activating authentication service "${authBackendTitle}" failed with status: ${error}`,
  }),
});

const loadBackendsPaginated = notifyingAction({
  action: AuthenticationActions.loadBackendsPaginated,
  error: (error) => ({
    message: `Loading authentication services failed with status: ${error}`,
  }),
});

const loadUsersPaginated = notifyingAction({
  action: AuthenticationActions.loadUsersPaginated,
  error: (authBackendId, error) => ({
    message: `Loading synchronized users for authentication service with id "${authBackendId}" failed with status: ${error}`,
  }),
});

const loadActiveBackendType = notifyingAction({
  action: AuthenticationActions.loadActiveBackendType,
  error: (error) => ({
    message: `Loading active authentication service type failed with status: ${error}`,
  }),
});

export default {
  create,
  update,
  load,
  loadActive,
  delete: deleteBackend,
  testConnection,
  testLogin,
  setActiveBackend,
  loadBackendsPaginated,
  loadUsersPaginated,
  loadActiveBackendType,
};
