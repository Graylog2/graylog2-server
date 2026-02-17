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
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

import type { CollectorsConfig, CollectorsConfigRequest, Fleet, Source } from '../types';

// Fleet API types
export type CreateFleetInput = {
  name: string;
  description: string;
  target_version?: string | null;
};
export type UpdateFleetInput = {
  fleetId: string;
  updates: Partial<Fleet>;
};

// Source API types
export type CreateSourceInput = {
  fleetId: string;
  source: Omit<Source, 'id' | 'fleet_id'>;
};
export type UpdateSourceInput = {
  fleetId: string;
  sourceId: string;
  updates: Omit<Source, 'id' | 'fleet_id'>;
};
export type DeleteSourceInput = {
  fleetId: string;
  sourceId: string;
};

// Fleet API functions
export const createFleet = async (input: CreateFleetInput): Promise<Fleet> =>
  fetch('POST', qualifyUrl('/collectors/fleets'), {
    name: input.name,
    description: input.description,
    target_version: input.target_version ?? null,
  });

export const updateFleet = async ({ fleetId, updates }: UpdateFleetInput): Promise<Fleet> =>
  fetch('PUT', qualifyUrl(`/collectors/fleets/${fleetId}`), {
    name: updates.name,
    description: updates.description,
    target_version: updates.target_version ?? null,
  });

export const deleteFleet = async (fleetId: string): Promise<void> =>
  fetch('DELETE', qualifyUrl(`/collectors/fleets/${fleetId}`));

// Source API functions
export const createSource = async ({ fleetId, source }: CreateSourceInput): Promise<Source> =>
  fetch('POST', qualifyUrl(`/collectors/fleets/${fleetId}/sources`), {
    name: source.name,
    description: source.description,
    enabled: source.enabled,
    config: source.config,
  });

export const updateSource = async ({ fleetId, sourceId, updates }: UpdateSourceInput): Promise<Source> =>
  fetch('PUT', qualifyUrl(`/collectors/fleets/${fleetId}/sources/${sourceId}`), {
    name: updates.name,
    description: updates.description,
    enabled: updates.enabled,
    config: updates.config,
  });

export const deleteSource = async ({ fleetId, sourceId }: DeleteSourceInput): Promise<void> =>
  fetch('DELETE', qualifyUrl(`/collectors/fleets/${fleetId}/sources/${sourceId}`));

// Enrollment Token API types
export type CreateEnrollmentTokenInput = {
  fleetId: string;
  expiresIn: string | null; // ISO-8601 duration: "PT24H", "P7D", "P30D", or null for default
};

export type EnrollmentTokenResponse = {
  token: string;
  expires_at: string; // ISO-8601 timestamp
};

// Enrollment Token API function
export const createEnrollmentToken = async (
  input: CreateEnrollmentTokenInput,
): Promise<EnrollmentTokenResponse> => {
  return fetchPeriodically('POST', qualifyUrl('/opamp/enrollment-tokens'), {
    fleet_id: input.fleetId,
    expires_in: input.expiresIn,
  });
};

export const fetchCollectorsConfig = async (): Promise<CollectorsConfig> => {
  return fetchPeriodically('GET', qualifyUrl('/collectors/config'));
};

export const updateCollectorsConfig = async (
  config: CollectorsConfigRequest,
): Promise<CollectorsConfig> => {
  return fetchPeriodically('PUT', qualifyUrl('/collectors/config'), config);
};
