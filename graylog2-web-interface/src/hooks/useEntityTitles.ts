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
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { create, windowScheduler } from '@yornaath/batshit';
import uniqBy from 'lodash/uniqBy';

import { SystemCatalog } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

export type EntityRef = { id: string; type: string };

const fetchEntityTitles = (entities: Array<EntityRef>) => SystemCatalog.getTitles({ entities: entities as any });

type TitlesResponse = Awaited<ReturnType<typeof fetchEntityTitles>>;

// Batches the requests of components rendered together, e.g. the cells of a table page.
const entityTitles = create({
  fetcher: (entities: Array<EntityRef>) => fetchEntityTitles(uniqBy(entities, ({ id, type }) => `${type}:${id}`)),
  resolver: (response: TitlesResponse, { id }: EntityRef) => ({
    id,
    title: response.entities?.find((entity) => entity.id === id)?.title,
    isPermitted: !response.not_permitted_to_view?.includes(id),
  }),
  scheduler: windowScheduler(10),
});

const useEntityTitles = (
  entities: Array<EntityRef>,
): {
  titlesById: Record<string, string>;
  notPermittedIds: Set<string>;
  isInitialLoading: boolean;
  isFetching: boolean;
  isError: boolean;
} => {
  const stableEntities = [...entities].sort((a, b) => a.id.localeCompare(b.id));
  const enabled = stableEntities.length > 0;

  const { data, isInitialLoading, isFetching, isError } = useQuery({
    queryKey: ['entity_titles', stableEntities],
    queryFn: () =>
      defaultOnError(
        Promise.all(stableEntities.map((entity) => entityTitles.fetch(entity))),
        'Loading entity titles failed with status',
        'Could not load entity titles',
      ),
    enabled,
    placeholderData: keepPreviousData,
  });

  const titlesById: Record<string, string> = Object.fromEntries(
    (data ?? []).filter(({ title }) => title !== undefined).map(({ id, title }) => [id, title]),
  );
  const notPermittedIds = new Set((data ?? []).filter(({ isPermitted }) => !isPermitted).map(({ id }) => id));

  return {
    titlesById,
    notPermittedIds,
    isInitialLoading,
    isFetching,
    isError,
  };
};

export default useEntityTitles;
