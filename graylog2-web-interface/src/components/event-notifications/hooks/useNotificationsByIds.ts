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
import { create, windowScheduler } from '@yornaath/batshit';

import { SystemCatalog } from '@graylog/server-api';

const ENTITY_TYPE = 'event_notifications';

export type ResolvedNotificationTitle = {
  id: string;
  title: string;
};

const fetchTitles = (ids: Array<string>) =>
  SystemCatalog.getTitles({
    entities: ids.map((id) => ({
      id,
      type: ENTITY_TYPE,
      identifier_field: undefined,
      identifier_type: undefined,
      display_fields: undefined,
      display_template: undefined,
    })),
  });

type TitlesResponse = Awaited<ReturnType<typeof fetchTitles>>;

// Batches the requests of components rendered together, e.g. the notifications cells of an event definitions page.
const notificationTitles = create({
  fetcher: (ids: Array<string>) => fetchTitles([...new Set(ids)]),
  resolver: (response: TitlesResponse, id: string) => ({
    id,
    entity: response.entities?.find((entity) => entity.id === id),
    isPermitted: !response.not_permitted_to_view?.includes(id),
  }),
  scheduler: windowScheduler(10),
});

type Result = {
  data: Array<ResolvedNotificationTitle> | undefined;
  notPermittedIds: Array<string>;
  isLoading: boolean;
};

const useNotificationsByIds = (ids: Array<string>): Result => {
  const sortedIds = [...new Set(ids)].sort();
  const { data, isLoading } = useQuery({
    queryKey: ['eventNotifications', 'titles', sortedIds],
    queryFn: () => Promise.all(sortedIds.map((id) => notificationTitles.fetch(id))),
    enabled: sortedIds.length > 0,
    staleTime: 30_000,
  });

  return {
    data: data?.flatMap(({ id, entity, isPermitted }) => (entity && isPermitted ? [{ id, title: entity.title }] : [])),
    notPermittedIds: data?.filter(({ isPermitted }) => !isPermitted).map(({ id }) => id) ?? [],
    isLoading,
  };
};

export default useNotificationsByIds;
