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
  createAuthBackend,
  loadAuthBackend,
  loadActiveAuthBackend,
  updateAuthBackend,
  deleteAuthBackend,
  testAuthConnection,
  testAuthLogin,
  setActiveAuthBackend,
  loadAuthBackendsPaginated,
  loadAuthBackendUsersPaginated,
  loadActiveAuthBackendType,
} from 'hooks/useAuthentication';

import notifyingAction from '../notifyingAction';

const create = notifyingAction({
  action: createAuthBackend,
  success: (authBackend) => ({
    message: `Authentication service "${authBackend.title} was created successfully`,
  }),
  error: (error, authBackend) => ({
    message: `Creating authentication service "${authBackend.title}" failed with status: ${error}`,
  }),
});

const load = notifyingAction({
  action: loadAuthBackend,
  error: (error, authBackendId) => ({
    message: `Loading authentication service with id "${authBackendId}" failed with status: ${error}`,
  }),
  notFoundRedirect: true,
});

const loadActive = notifyingAction({
  action: loadActiveAuthBackend,
  error: (error) => ({
    message: `Loading active authentication service failed with status: ${error}`,
  }),
});

const update = notifyingAction({
  action: updateAuthBackend,
  success: (_authBackendId, authBackend) => ({
    message: `Authentication service "${authBackend.title}" was updated successfully`,
  }),
  error: (error, _authBackendId, authBackend) => ({
    message: `Updating authentication service "${authBackend.title}" failed with status: ${error}`,
  }),
});

const deleteBackend = notifyingAction({
  action: (backendId: string, _authBackendTitle: string) => deleteAuthBackend(backendId),
  success: (_authBackendId, authBackendTitle) => ({
    message: `Authentication service "${authBackendTitle} was deleted successfully`,
  }),
  error: (error, _authBackendId, authBackendTitle) => ({
    message: `Deleting authentication service "${authBackendTitle}" failed with status: ${error}`,
  }),
});

const testConnection = notifyingAction({
  action: testAuthConnection,
  error: (error) => ({
    message: `Connection test failed with status: ${error}`,
  }),
});

const testLogin = notifyingAction({
  action: testAuthLogin,
  error: (error) => ({
    message: `Login test failed with status: ${error}`,
  }),
});

const setActiveBackend = notifyingAction({
  action: (backendId: string, _authBackendTitle: string) => setActiveAuthBackend(backendId),
  success: (authBackendId, authBackendTitle) => ({
    message: `Authentication service "${authBackendTitle} was ${authBackendId ? 'activated' : 'deactivated'} successfully`,
  }),
  error: (error, _authBackendId, authBackendTitle) => ({
    message: `Activating authentication service "${authBackendTitle}" failed with status: ${error}`,
  }),
});

const loadBackendsPaginated = notifyingAction({
  action: loadAuthBackendsPaginated,
  error: (error) => ({
    message: `Loading authentication services failed with status: ${error}`,
  }),
});

const loadUsersPaginated = notifyingAction({
  action: loadAuthBackendUsersPaginated,
  error: (authBackendId, error) => ({
    message: `Loading synchronized users for authentication service with id "${authBackendId}" failed with status: ${error}`,
  }),
});

const loadActiveBackendType = notifyingAction({
  action: loadActiveAuthBackendType,
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
