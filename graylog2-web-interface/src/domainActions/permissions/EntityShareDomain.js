// @flow strict
import type { ActionsType } from 'actions/permissions/EntityShareActions';
import { EntityShareActions } from 'stores/permissions/EntityShareStore';

import notifyingAction from '../notifyingAction';

const prepare: $PropertyType<ActionsType, 'prepare'> = notifyingAction({
  action: EntityShareActions.prepare,
  error: (error, entityName, entityType) => ({
    message: `Preparing shares for ${entityType} "${entityName}" failed with status: ${error}`,
  }),
});

const update: $PropertyType<ActionsType, 'update'> = notifyingAction({
  action: EntityShareActions.update,
  error: (error, entityName, entityType) => ({
    message: `Updating shares for ${entityType} "${entityName}" failed with status: ${error}`,
  }),
  success: (entityName, entityType) => ({
    message: `Shares for ${entityType} "${entityName}" updated successfully`,
  }),
});

const loadUserSharesPaginated: $PropertyType<ActionsType, 'loadUserSharesPaginated'> = notifyingAction({
  action: EntityShareActions.loadUserSharesPaginated,
  error: (error, userId) => ({
    message: `Loading entities which got shared for user with id "${userId}" failed with status: ${error}`,
  }),
});

export default {
  prepare,
  update,
  loadUserSharesPaginated,
};
