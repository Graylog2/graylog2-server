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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import UserNotification from 'util/UserNotification';

import {
  createFleet,
  updateFleet,
  deleteFleet,
  createSource,
  updateSource,
  deleteSource,
  createEnrollmentToken,
} from './collectorsApi';

const COLLECTORS_QUERY_KEY = ['collectors'];

const useCollectorsMutations = () => {
  const queryClient = useQueryClient();

  const invalidateCollectorsQueries = () =>
    queryClient.invalidateQueries({ queryKey: COLLECTORS_QUERY_KEY });

  // Fleet mutations
  const createFleetMutation = useMutation({
    mutationFn: createFleet,
    onError: (errorThrown) => {
      UserNotification.error(
        `Creating fleet failed: ${errorThrown}`,
        'Could not create fleet',
      );
    },
    onSuccess: (fleet) => {
      UserNotification.success(`Fleet "${fleet.name}" has been created.`, 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  const updateFleetMutation = useMutation({
    mutationFn: updateFleet,
    onError: (errorThrown) => {
      UserNotification.error(
        `Updating fleet failed: ${errorThrown}`,
        'Could not update fleet',
      );
    },
    onSuccess: (fleet) => {
      UserNotification.success(`Fleet "${fleet.name}" has been updated.`, 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  const deleteFleetMutation = useMutation({
    mutationFn: deleteFleet,
    onError: (errorThrown) => {
      UserNotification.error(
        `Deleting fleet failed: ${errorThrown}`,
        'Could not delete fleet',
      );
    },
    onSuccess: () => {
      UserNotification.success('Fleet has been deleted.', 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  // Source mutations
  const createSourceMutation = useMutation({
    mutationFn: createSource,
    onError: (errorThrown) => {
      UserNotification.error(
        `Creating source failed: ${errorThrown}`,
        'Could not create source',
      );
    },
    onSuccess: (source) => {
      UserNotification.success(`Source "${source.name}" has been created.`, 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  const updateSourceMutation = useMutation({
    mutationFn: updateSource,
    onError: (errorThrown) => {
      UserNotification.error(
        `Updating source failed: ${errorThrown}`,
        'Could not update source',
      );
    },
    onSuccess: (source) => {
      UserNotification.success(`Source "${source.name}" has been updated.`, 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  const deleteSourceMutation = useMutation({
    mutationFn: deleteSource,
    onError: (errorThrown) => {
      UserNotification.error(
        `Deleting source failed: ${errorThrown}`,
        'Could not delete source',
      );
    },
    onSuccess: () => {
      UserNotification.success('Source has been deleted.', 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  // Enrollment token mutation
  const createEnrollmentTokenMutation = useMutation({
    mutationFn: createEnrollmentToken,
    onError: (errorThrown) => {
      UserNotification.error(
        `Creating enrollment token failed: ${errorThrown}`,
        'Could not create token',
      );
    },
  });

  return {
    // Fleet operations
    createFleet: createFleetMutation.mutateAsync,
    isCreatingFleet: createFleetMutation.isPending,
    updateFleet: updateFleetMutation.mutateAsync,
    isUpdatingFleet: updateFleetMutation.isPending,
    deleteFleet: deleteFleetMutation.mutateAsync,
    isDeletingFleet: deleteFleetMutation.isPending,

    // Source operations
    createSource: createSourceMutation.mutateAsync,
    isCreatingSource: createSourceMutation.isPending,
    updateSource: updateSourceMutation.mutateAsync,
    isUpdatingSource: updateSourceMutation.isPending,
    deleteSource: deleteSourceMutation.mutateAsync,
    isDeletingSource: deleteSourceMutation.isPending,

    // Enrollment token operations
    createEnrollmentToken: createEnrollmentTokenMutation.mutateAsync,
    isCreatingEnrollmentToken: createEnrollmentTokenMutation.isPending,
  };
};

export default useCollectorsMutations;
