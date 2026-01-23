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
import { useQuery } from '@tanstack/react-query';

import type { Fleet, CollectorInstanceView, Source, CollectorStats } from '../types';
import {
  mockFleets,
  mockInstances,
  mockSources,
  mockStats,
  getFleetById,
  getInstancesByFleetId,
  getSourcesByFleetId,
  getFleetStats,
} from '../mockData';

const QUERY_KEY_PREFIX = 'collectors';

// Simulate API delay
const delay = (ms: number) => new Promise<void>((resolve) => {
  setTimeout(resolve, ms);
});

export const useCollectorStats = () =>
  useQuery<CollectorStats>({
    queryKey: [QUERY_KEY_PREFIX, 'stats'],
    queryFn: async () => {
      await delay(200);

      return mockStats;
    },
  });

export const useFleets = () =>
  useQuery<Fleet[]>({
    queryKey: [QUERY_KEY_PREFIX, 'fleets'],
    queryFn: async () => {
      await delay(200);

      return mockFleets;
    },
  });

export const useFleet = (fleetId: string) =>
  useQuery<Fleet | undefined>({
    queryKey: [QUERY_KEY_PREFIX, 'fleets', fleetId],
    queryFn: async () => {
      await delay(200);

      return getFleetById(fleetId);
    },
    enabled: !!fleetId,
  });

export const useFleetStats = (fleetId: string) =>
  useQuery({
    queryKey: [QUERY_KEY_PREFIX, 'fleets', fleetId, 'stats'],
    queryFn: async () => {
      await delay(100);

      return getFleetStats(fleetId);
    },
    enabled: !!fleetId,
  });

export const useInstances = (fleetId?: string) =>
  useQuery<CollectorInstanceView[]>({
    queryKey: [QUERY_KEY_PREFIX, 'instances', { fleetId }],
    queryFn: async () => {
      await delay(200);

      return fleetId ? getInstancesByFleetId(fleetId) : mockInstances;
    },
  });

export const useSources = (fleetId?: string) =>
  useQuery<Source[]>({
    queryKey: [QUERY_KEY_PREFIX, 'sources', { fleetId }],
    queryFn: async () => {
      await delay(200);

      return fleetId ? getSourcesByFleetId(fleetId) : mockSources;
    },
  });
