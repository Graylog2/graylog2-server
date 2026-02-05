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
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

import type { Fleet, Source } from '../types';
import { mockFleets, mockSources } from '../mockData';

// Simulate network delay
const delay = (ms: number) => new Promise((resolve) => { setTimeout(resolve, ms); });
const MOCK_DELAY = 300;

// Fleet API types
export type CreateFleetInput = Omit<Fleet, 'id' | 'created_at' | 'updated_at'>;
export type UpdateFleetInput = { fleetId: string; updates: Partial<Fleet> };

// Source API types
export type CreateSourceInput = Omit<Source, 'id'>;
export type UpdateSourceInput = { sourceId: string; updates: Partial<Source> };

// Fleet API functions
export const createFleet = async (input: CreateFleetInput): Promise<Fleet> => {
  await delay(MOCK_DELAY);

  const now = new Date().toISOString();
  const newFleet: Fleet = {
    ...input,
    id: `fleet-${Date.now()}`,
    created_at: now,
    updated_at: now,
  };

  mockFleets.push(newFleet);

  return newFleet;
};

export const updateFleet = async ({ fleetId, updates }: UpdateFleetInput): Promise<Fleet> => {
  await delay(MOCK_DELAY);

  const index = mockFleets.findIndex((f) => f.id === fleetId);

  if (index === -1) {
    throw new Error(`Fleet not found: ${fleetId}`);
  }

  const updatedFleet: Fleet = {
    ...mockFleets[index],
    ...updates,
    id: fleetId, // Prevent id from being overwritten
    updated_at: new Date().toISOString(),
  };

  mockFleets[index] = updatedFleet;

  return updatedFleet;
};

export const deleteFleet = async (fleetId: string): Promise<void> => {
  await delay(MOCK_DELAY);

  const index = mockFleets.findIndex((f) => f.id === fleetId);

  if (index === -1) {
    throw new Error(`Fleet not found: ${fleetId}`);
  }

  // Remove the fleet
  mockFleets.splice(index, 1);

  // Also delete all sources belonging to this fleet
  for (let i = mockSources.length - 1; i >= 0; i -= 1) {
    if (mockSources[i].fleet_id === fleetId) {
      mockSources.splice(i, 1);
    }
  }
};

// Source API functions
export const createSource = async (input: CreateSourceInput): Promise<Source> => {
  await delay(MOCK_DELAY);

  const newSource = {
    ...input,
    id: `src-${Date.now()}`,
  } as Source;

  mockSources.push(newSource);

  return newSource;
};

export const updateSource = async ({ sourceId, updates }: UpdateSourceInput): Promise<Source> => {
  await delay(MOCK_DELAY);

  const index = mockSources.findIndex((s) => s.id === sourceId);

  if (index === -1) {
    throw new Error(`Source not found: ${sourceId}`);
  }

  const updatedSource = {
    ...mockSources[index],
    ...updates,
    id: sourceId, // Prevent id from being overwritten
  } as Source;

  mockSources[index] = updatedSource;

  return updatedSource;
};

export const deleteSource = async (sourceId: string): Promise<void> => {
  await delay(MOCK_DELAY);

  const index = mockSources.findIndex((s) => s.id === sourceId);

  if (index === -1) {
    throw new Error(`Source not found: ${sourceId}`);
  }

  mockSources.splice(index, 1);
};

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
