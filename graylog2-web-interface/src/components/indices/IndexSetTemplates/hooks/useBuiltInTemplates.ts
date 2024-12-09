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

const fetchBuiltInIndexSetTemplates = async (warmTierEnabled: boolean) => {
  const url = qualifyUrl(`/system/indices/index_sets/templates/built-in?warm_tier_enabled=${warmTierEnabled}`);

  return fetch('GET', url).then((data: Readonly<Array<IndexSetTemplate>>) => (data));
};

const useBuiltInTemplates = (warmTierEnabled: boolean, { enabled } = { enabled: true }): {
  data: Readonly<Array<IndexSetTemplate>>,
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    ['indexSetTemplatesBuiltIn', warmTierEnabled],
    () => defaultOnError(fetchBuiltInIndexSetTemplates(warmTierEnabled), 'Loading built in index set templates failed with status', 'Could not load built in index set templates'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data: data ?? [],
    isLoading,
    refetch,
  });
};

export default useBuiltInTemplates;
