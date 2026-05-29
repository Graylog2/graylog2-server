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

type Result = {
  data: Array<ResolvedNotificationTitle> | undefined;
  notPermittedIds: Array<string>;
  isLoading: boolean;
};

const useNotificationsByIds = (ids: Array<string>): Result => {
  const sortedIds = [...new Set(ids)].sort();
  const { data, isLoading } = useQuery({
    queryKey: ['eventNotifications', 'titles', sortedIds],
    queryFn: () => fetchTitles(sortedIds),
    enabled: sortedIds.length > 0,
    staleTime: 30_000,
  });

  const notPermittedIds = data?.not_permitted_to_view ?? [];

  return {
    data: data?.entities?.filter(({ id }) => !notPermittedIds.includes(id)).map(({ id, title }) => ({ id, title })),
    notPermittedIds,
    isLoading,
  };
};

export default useNotificationsByIds;
