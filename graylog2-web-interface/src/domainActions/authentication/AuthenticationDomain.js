// @flow strict
import type { ActionsType } from 'actions/authentication/AuthenticationActions';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';

import notifyingAction from '../notifyingAction';

const create: $PropertyType<ActionsType, 'create'> = notifyingAction({
  action: AuthenticationActions.create,
  success: (authBackend) => ({
    message: `Authentication service "${authBackend.title} was created successfully"`,
  }),
  error: (error, authBackend) => ({
    message: `Creating authentication service "${authBackend.title}" failed with status: ${error}`,
  }),
});

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: AuthenticationActions.load,
  error: (error, authBackendId) => ({
    message: `Loading authentication service with id "${authBackendId}" failed with status: ${error}`,
  }),
});

const update: $PropertyType<ActionsType, 'update'> = notifyingAction({
  action: AuthenticationActions.update,
  success: (authBackendId, authBackend) => ({
    message: `Authentication service "${authBackend.title} was updated successfully"`,
  }),
  error: (error, authBackendId, authBackend) => ({
    message: `Updating authentication service "${authBackend.title}" failed with status: ${error}`,
  }),
});

const deleteBackend: $PropertyType<ActionsType, 'delete'> = notifyingAction({
  action: AuthenticationActions.delete,
  success: (authBackendId, authBackendTitle) => ({
    message: `Authentication service "${authBackendTitle} was deleted successfully"`,
  }),
  error: (error, authBackendId, authBackendTitle) => ({
    message: `Deleting authentication service "${authBackendTitle}" failed with status: ${error}`,
  }),
});

const testConnection: $PropertyType<ActionsType, 'testConnection'> = notifyingAction({
  action: AuthenticationActions.testConnection,
  error: (error) => ({
    message: `Connection test failed with status: ${error}`,
  }),
});

const testLogin: $PropertyType<ActionsType, 'testLogin'> = notifyingAction({
  action: AuthenticationActions.testLogin,
  error: (error) => ({
    message: `Login test failed with status: ${error}`,
  }),
});

const enableUser: $PropertyType<ActionsType, 'enableUser'> = notifyingAction({
  action: AuthenticationActions.enableUser,
  success: (userId, username) => ({
    message: `User "${username} was enabled successfully"`,
  }),
  error: (error, userId, username) => ({
    message: `Enabling user "${username}" failed with status: ${error}`,
  }),
});

const disableUser: $PropertyType<ActionsType, 'disableUser'> = notifyingAction({
  action: AuthenticationActions.disableUser,
  success: (userId, username) => ({
    message: `User "${username} was disabled successfully"`,
  }),
  error: (error, userId, username) => ({
    message: `Disabling user "${username}" failed with status: ${error}`,
  }),
});

const setActiveBackend: $PropertyType<ActionsType, 'setActiveBackend'> = notifyingAction({
  action: AuthenticationActions.setActiveBackend,
  success: (authBackendId, authBackendTitle) => ({
    message: `Authentication service "${authBackendTitle} was disabled successfully"`,
  }),
  error: (error, authBackendId, authBackendTitle) => ({
    message: `Activating authentication service "${authBackendTitle}" failed with status: ${error}`,
  }),
});

const loadBackendsPaginated: $PropertyType<ActionsType, 'loadBackendsPaginated'> = notifyingAction({
  action: AuthenticationActions.loadBackendsPaginated,
  error: (error) => ({
    message: `Loading authentication services failed with status: ${error}`,
  }),
});

const loadUsersPaginated: $PropertyType<ActionsType, 'loadUsersPaginated'> = notifyingAction({
  action: AuthenticationActions.loadUsersPaginated,
  error: (error) => ({
    message: `Loading synchronized users failed with status: ${error}`,
  }),
});

export default {
  create,
  update,
  load,
  delete: deleteBackend,
  testConnection,
  testLogin,
  enableUser,
  disableUser,
  setActiveBackend,
  loadBackendsPaginated,
  loadUsersPaginated,
};
