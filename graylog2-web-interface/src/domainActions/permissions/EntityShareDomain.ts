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
import type { Optional } from 'utility-types';

import { prepareEntityShare, updateEntityShare, loadUserSharesPaginated } from 'api/entity-share';
import type { GRN } from 'logic/permissions/types';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import notifyingAction from '../notifyingAction';

const prepare = notifyingAction({
  action: (_entityType: string, _entityTitle: string, entityGRN: GRN | null, payload?: Optional<EntitySharePayload>) =>
    prepareEntityShare(entityGRN, payload),
  error: (error, entityType, entityName) => ({
    message: `Preparing shares for ${entityType} "${entityName}" failed with status: ${error}`,
  }),
});

const update = notifyingAction({
  action: (_entityType: string, _entityTitle: string, entityGRN: GRN, payload: EntitySharePayload) =>
    updateEntityShare(entityGRN, payload),
  error: (error, entityType, entityName) => ({
    message: `Updating shares for ${entityType} "${entityName}" failed with status: ${error}`,
  }),
  success: (entityType, entityName) => ({
    message: `Shares for ${entityType} "${entityName}" updated successfully`,
  }),
});

const loadUserSharesPaginatedAction = notifyingAction({
  action: (userId: string, pagination) => loadUserSharesPaginated(userId, pagination),
  error: (error, userId) => ({
    message: `Loading entities which got shared for user with id "${userId}" failed with status: ${error}`,
  }),
});

export default {
  prepare,
  update,
  loadUserSharesPaginated: loadUserSharesPaginatedAction,
};
