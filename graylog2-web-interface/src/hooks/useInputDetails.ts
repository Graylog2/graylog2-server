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
import { useMemo } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import useEntityTitles from 'hooks/useEntityTitles';
import usePluginEntities from 'hooks/usePluginEntities';
import { defaultOnError } from 'util/conditional/onError';

import './types';

/**
 * Map of input-type discriminator → the extra fields that variant carries.
 *
 * Open-source declares only the regular {@code input} variant (id + title via the catalog). Plugins
 * (e.g. enterprise forwarder) extend the map by augmenting this interface via
 * {@code declare module 'hooks/useInputDetails'}, adding their own discriminator + fields. The
 * resulting {@link ResolvedInput} union picks up the new variant automatically.
 */
export interface ResolvedInputMap {
  input: { id: string; title: string };
}

export type ResolvedInput = {
  [K in keyof ResolvedInputMap]: ResolvedInputMap[K] & { type: K };
}[keyof ResolvedInputMap];

/**
 * Fetches title (and any type-specific extras) for input IDs of one type that isn't covered by the
 * default catalog endpoint. Registered via the {@code inputDetailsFetchers} plugin store key.
 */
export type InputDetailsFetcher = {
  type: string;
  fetchDetails: (ids: Array<string>) => Promise<Array<ResolvedInput>>;
};

export type TypedInput = { id: string; type: string };

const useInputDetails = (
  typedInputs: ReadonlyArray<TypedInput>,
): {
  resolvedById: Record<string, ResolvedInput>;
  isInitialLoading: boolean;
  /**
   * True while any underlying request is in flight, including background refetches. Use this to
   * distinguish "title hasn't arrived yet" from "input is gone" — a missing entry in
   * {@link resolvedById} only means deleted once {@code isFetching} has settled.
   */
  isFetching: boolean;
  isError: boolean;
} => {
  const regularIds = useMemo(() => typedInputs.filter((t) => t.type === 'input').map((t) => t.id), [typedInputs]);
  const titleEntities = useMemo(() => regularIds.map((id) => ({ id, type: 'inputs' })), [regularIds]);
  const {
    titlesById,
    isInitialLoading: isCatalogLoading,
    isFetching: isCatalogFetching,
    isError: isCatalogError,
  } = useEntityTitles(titleEntities);

  // Plugin-registered fetchers handle non-default input types (currently: forwarder_input).
  const fetchers = usePluginEntities('inputDetailsFetchers') ?? [];
  const nonDefaultIdsByType = useMemo(() => {
    const grouped = new Map<string, Array<string>>();
    typedInputs.forEach(({ id, type }) => {
      if (type === 'input') return;
      const existing = grouped.get(type) ?? [];
      existing.push(id);
      grouped.set(type, existing);
    });

    return grouped;
  }, [typedInputs]);

  const enabled = nonDefaultIdsByType.size > 0;
  const queryKey = useMemo(
    () => [
      'input_details',
      Array.from(nonDefaultIdsByType.entries())
        .map(([type, ids]) => `${type}:${[...ids].sort().join(',')}`)
        .sort(),
    ],
    [nonDefaultIdsByType],
  );

  const {
    data: pluginResolved,
    isInitialLoading: isPluginLoading,
    isFetching: isPluginFetching,
    isError: isPluginError,
  } = useQuery({
    queryKey,
    queryFn: () =>
      defaultOnError(
        Promise.all(
          fetchers.map((fetcher) => {
            const ids = nonDefaultIdsByType.get(fetcher.type) ?? [];

            return ids.length > 0 ? fetcher.fetchDetails(ids) : Promise.resolve([]);
          }),
        ).then((chunks) => chunks.flat()),
        'Loading input details failed with status',
        'Could not load input details',
      ),
    enabled,
    placeholderData: keepPreviousData,
  });

  const resolvedById = useMemo(() => {
    const out: Record<string, ResolvedInput> = {};
    regularIds.forEach((id) => {
      const title = titlesById[id];
      if (title !== undefined) {
        out[id] = { type: 'input', id, title } as ResolvedInput;
      }
    });
    (pluginResolved ?? []).forEach((resolved) => {
      out[resolved.id] = resolved;
    });

    return out;
  }, [regularIds, titlesById, pluginResolved]);

  return {
    resolvedById,
    isInitialLoading: isCatalogLoading || isPluginLoading,
    isFetching: isCatalogFetching || isPluginFetching,
    isError: isCatalogError || isPluginError,
  };
};

export default useInputDetails;
