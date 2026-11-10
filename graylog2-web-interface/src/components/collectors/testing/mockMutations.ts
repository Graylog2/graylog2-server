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

import type useCollectorsMutations from '../hooks/useCollectorsMutations';

const noop = jest.fn();

export const mockCollectorsMutations = (
  overrides: Partial<ReturnType<typeof useCollectorsMutations>> = {},
): ReturnType<typeof useCollectorsMutations> => ({
  createFleet: noop,
  isCreatingFleet: false,
  updateFleet: noop,
  isUpdatingFleet: false,
  deleteFleet: noop,
  isDeletingFleet: false,
  createSource: noop,
  isCreatingSource: false,
  updateSource: noop,
  isUpdatingSource: false,
  deleteSource: noop,
  isDeletingSource: false,
  createEnrollmentToken: noop,
  isCreatingEnrollmentToken: false,
  deleteEnrollmentToken: noop,
  isDeletingEnrollmentToken: false,
  bulkDeleteEnrollmentTokens: noop,
  isBulkDeletingEnrollmentTokens: false,
  reassignInstances: noop,
  isReassigningInstances: false,
  deleteInstance: noop,
  isDeletingInstance: false,
  updateConfig: noop,
  isUpdatingConfig: false,
  ...overrides,
});
