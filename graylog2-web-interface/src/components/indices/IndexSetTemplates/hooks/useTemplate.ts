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

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type {
  IndexSetTemplate,
} from 'components/indices/IndexSetTemplates/types';

const INITIAL_DATA: IndexSetTemplate = {
  title: null,
  id: null,
  description: null,
  built_in: false,
  index_set_config: null,
};

const fetchIndexSetTemplate = async (id: string) => {
  const url = qualifyUrl(`/system/indices/index_sets/templates/${id}`);

  return fetch('GET', url).then((template: IndexSetTemplate) => ({
    id: template.id,
    title: template.title,
    description: template.description,
    built_in: template.built_in,
    index_set_config: template.index_set_config,
  }));
};

const useTemplate = (id: string): {
  data: IndexSetTemplate,
  isFetched: boolean,
  isFetching: boolean,
  refetch: () => void,
} => {
  const { data, isFetched, isFetching, refetch } = useQuery(
    ['indexSetTemplate', id],
    () => fetchIndexSetTemplate(id),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index set template failed with status: ${errorThrown}`,
          'Could not load index fset template');
      },
      keepPreviousData: true,
      enabled: !!id,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isFetched,
    isFetching,
    refetch,
  });
};

export default useTemplate;
