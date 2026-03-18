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
import {useMutation, useQueryClient} from '@tanstack/react-query';

import { Collectors, CollectorsFleets, CollectorsSources, CollectorsConfig as CollectorsConfigApi, OpAMPEnrollment } from '@graylog/server-api';

import request from 'routing/request';
import UserNotification from 'util/UserNotification';

import type {CollectorsConfigRequest, Fleet, Source} from '../types';

type CreateSourceInput = {
  fleetId: string;
  source: Omit<Source, 'id' | 'fleet_id'>;
};
type UpdateSourceInput = {
  fleetId: string;
  sourceId: string;
  updates: Omit<Source, 'id' | 'fleet_id'>;
};

const useCollectorsMutations = () => {
  const queryClient = useQueryClient();

  const invalidateCollectorsQueries = () =>
    queryClient.invalidateQueries({ queryKey: ['collectors'] });

  // Fleet mutations
  const createFleetMutation = useMutation({
    mutationFn: (input: { name: string; description?: string; target_version?: string | null }) =>
      CollectorsFleets.create({ name: input.name, description: input.description, target_version: input.target_version ?? null }),
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
    mutationFn: ({ fleetId, updates }: { fleetId: string; updates: Partial<Fleet> }) =>
      CollectorsFleets.update(fleetId, {
        name: updates.name,
        description: updates.description,
        target_version: updates.target_version ?? null,
      }),
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
    mutationFn: (fleetId: string) => CollectorsFleets.remove(fleetId),
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
    mutationFn: ({ fleetId, source }: CreateSourceInput) =>
      CollectorsSources.create(fleetId, {
        name: source.name,
        description: source.description,
        enabled: source.enabled,
        config: { type: source.type, ...source.config },
      }) as Promise<Source>,
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
    mutationFn: ({ fleetId, sourceId, updates }: UpdateSourceInput) =>
      CollectorsSources.update(fleetId, sourceId, {
        name: updates.name,
        description: updates.description,
        enabled: updates.enabled,
        config: { type: updates.type, ...updates.config },
      }) as Promise<Source>,
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
    mutationFn: ({ fleetId, sourceId }: { fleetId: string; sourceId: string }) =>
      CollectorsSources.remove(fleetId, sourceId),
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

  // Enrollment token mutations
  const createEnrollmentTokenMutation = useMutation({
    mutationFn: (input: { fleetId: string; expiresIn: string | null }) =>
      OpAMPEnrollment.createToken({
        fleet_id: input.fleetId,
        expires_in: input.expiresIn,
      }),
    onError: (errorThrown) => {
      UserNotification.error(
        `Creating enrollment token failed: ${errorThrown}`,
        'Could not create token',
      );
    },
    onSuccess: () => invalidateCollectorsQueries(),
  });

  const deleteEnrollmentTokenMutation = useMutation({
    mutationFn: (tokenId: string) => OpAMPEnrollment.remove(tokenId),
    onError: (errorThrown) => {
      UserNotification.error(
        `Deleting enrollment token failed: ${errorThrown}`,
        'Could not delete token',
      );
    },
    onSuccess: () => {
      UserNotification.success('Enrollment token has been deleted.', 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  // Instance reassignment mutation
  // TODO: Replace with generated API client after regeneration (Collectors.reassignInstances)
  const reassignInstancesMutation = useMutation({
    mutationFn: (input: { instanceUids: string[]; fleetId: string }) =>
      request('POST', '/collectors/instances/reassign', {
        instance_uids: input.instanceUids,
        fleet_id: input.fleetId,
      }, {}, { Accept: 'application/json' }),
    onError: (errorThrown) => {
      UserNotification.error(
        `Reassigning instances failed: ${errorThrown}`,
        'Could not reassign instances',
      );
    },
    onSuccess: () => {
      UserNotification.success('Instances have been reassigned.', 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  // Instance delete mutation
  const deleteInstanceMutation = useMutation({
    mutationFn: (instanceUid: string) => Collectors.deleteInstance(instanceUid),
    onError: (errorThrown) => {
      UserNotification.error(
        `Deleting instance failed: ${errorThrown}`,
        'Could not delete instance',
      );
    },
    onSuccess: () => {
      UserNotification.success('Instance has been deleted.', 'Success!');

      return invalidateCollectorsQueries();
    },
  });

  // Config mutation
  const updateConfigMutation = useMutation({
    mutationFn: (config: CollectorsConfigRequest) => CollectorsConfigApi.put(config),
    onError: (errorThrown: { additional?: { body?: { validation_errors?: Record<string, Array<{ error: string }>> } } }) => {
      const validationErrors = errorThrown?.additional?.body?.validation_errors;

      if (validationErrors) {
        // Don't show toast for validation errors — form will display them inline
        return;
      }

      UserNotification.error(
        `Saving collectors config failed: ${errorThrown}`,
        'Could not save config',
      );
    },
    onSuccess: () => {
      UserNotification.success('Collectors config saved.', 'Success!');

      return invalidateCollectorsQueries();
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
    deleteEnrollmentToken: deleteEnrollmentTokenMutation.mutateAsync,
    isDeletingEnrollmentToken: deleteEnrollmentTokenMutation.isPending,

    // Instance operations
    reassignInstances: reassignInstancesMutation.mutateAsync,
    isReassigningInstances: reassignInstancesMutation.isPending,
    deleteInstance: deleteInstanceMutation.mutateAsync,
    isDeletingInstance: deleteInstanceMutation.isPending,

    // Config operations
    updateConfig: updateConfigMutation.mutateAsync,
    isUpdatingConfig: updateConfigMutation.isPending,
  };
};

export default useCollectorsMutations;
