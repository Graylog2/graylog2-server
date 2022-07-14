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
import SharedEntity from 'logic/permissions/SharedEntity';
import { EntityShareActions } from 'stores/permissions/EntityShareStore';

import notifyingAction from '../notifyingAction';

const prepare = notifyingAction({
  action: EntityShareActions.prepare,
  error: (error, entityType, entityName) => ({
    message: `Preparing shares for ${SharedEntity.getReadableType(entityType)} "${entityName}" failed with status: ${error}`,
  }),
});

const update = notifyingAction({
  action: EntityShareActions.update,
  error: (error, entityType, entityName) => ({
    message: `Updating shares for ${SharedEntity.getReadableType(entityType)} "${entityName}" failed with status: ${error}`,
  }),
  success: (entityType, entityName) => ({
    message: `Shares for ${SharedEntity.getReadableType(entityType)} "${entityName}" updated successfully`,
  }),
});

const loadUserSharesPaginated = notifyingAction({
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
