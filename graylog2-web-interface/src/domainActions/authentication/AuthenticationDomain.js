// @flow strict
import type { ActionsType } from 'actions/authentication/AuthenticationActions';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';

import notifyingAction from '../notifyingAction';

const create: $PropertyType<ActionsType, 'create'> = notifyingAction({
  action: AuthenticationActions.create,
  success: (authBackend) => ({
    message: `Authentication backend "${authBackend.title} was created successfully"`,
  }),
  error: (error, authBackend) => ({
    message: `Creating authentication backend ${authBackend.title} failed with status: ${error}`,
  }),
});

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: AuthenticationActions.load,
  error: (error, authBackendId) => ({
    message: `Loading authentication backend with id "${authBackendId}" failed with status: ${error}`,
  }),
});

const testConnetion: $PropertyType<ActionsType, 'testConnetion'> = notifyingAction({
  action: AuthenticationActions.testConnetion,
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

const loadBackendsPaginated: $PropertyType<ActionsType, 'loadBackendsPaginated'> = notifyingAction({
  action: AuthenticationActions.loadBackendsPaginated,
  error: (error) => ({
    message: `Loading authentication backends failed with status: ${error}`,
  }),
});

const loadUsersPaginated: $PropertyType<ActionsType, 'loadUsersPaginated'> = notifyingAction({
  action: AuthenticationActions.loadUsersPaginated,
  error: (error) => ({
    message: `Loading synchronised users failed with status: ${error}`,
  }),
});

const enableUser: $PropertyType<ActionsType, 'enableUser'> = notifyingAction({
  action: AuthenticationActions.enableUser,
  success: (userId, username) => ({
    message: `User "${username} was enabled successfully"`,
  }),
  error: (error, userId, username) => ({
    message: `Enabling user ${username} failed with status: ${error}`,
  }),
});

const disableUser: $PropertyType<ActionsType, 'disableUser'> = notifyingAction({
  action: AuthenticationActions.disableUser,
  success: (userId, username) => ({
    message: `User "${username} was disabled successfully"`,
  }),
  error: (error, userId, username) => ({
    message: `Disabling user ${username} failed with status: ${error}`,
  }),
});

export default {
  create,
  load,
  testConnetion,
  testLogin,
  loadBackendsPaginated,
  loadUsersPaginated,
  enableUser,
  disableUser,
};
