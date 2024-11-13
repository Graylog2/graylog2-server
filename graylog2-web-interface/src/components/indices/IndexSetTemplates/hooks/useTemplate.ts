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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type {
  IndexSetTemplate,
} from 'components/indices/IndexSetTemplates/types';
import { defaultOnError } from 'util/conditional/onError';

const INITIAL_DATA: IndexSetTemplate = {
  title: null,
  id: null,
  description: null,
  built_in: false,
  default: false,
  enabled: true,
  disabled_reason: '',
  index_set_config: null,
};

const fetchIndexSetTemplate = async (id: string) => {
  const url = qualifyUrl(`/system/indices/index_sets/templates/${id}`);

  return fetch('GET', url).then((template: IndexSetTemplate) => (template));
};

const useTemplate = (id: string): {
  data: IndexSetTemplate,
  isFetched: boolean,
  isFetching: boolean,
  isSuccess: boolean,
  isError: boolean,
  refetch: () => void,
} => {
  const { data, isFetched, isFetching, isSuccess, isError, refetch } = useQuery(
    ['indexSetTemplate', id],
    () => defaultOnError(fetchIndexSetTemplate(id), 'Loading index set template failed with status', 'Could not load index set template'),
    {
      keepPreviousData: true,
      enabled: !!id,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isFetched,
    isFetching,
    isSuccess,
    isError,
    refetch,
  });
};

export default useTemplate;
