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
import type { IndexSetsDefaultConfiguration } from 'components/indices/IndexSetTemplates/types';
import { defaultOnError } from 'util/conditional/onError';

const fetchIndexSetTemplateDefaults: () => Promise<IndexSetsDefaultConfiguration> = () => fetch('GET', qualifyUrl('/system/indices/index_sets/templates/default_config'));

const useIndexSetTemplateDefaults = () => {
  const { data, isLoading } = useQuery<IndexSetsDefaultConfiguration, Error>(
    ['index-templates-defaults'],
    () => defaultOnError(fetchIndexSetTemplateDefaults(), 'Error fetching index default template'),
    {
      retry: 1,
    },
  );

  return {
    loadingIndexSetTemplateDefaults: isLoading,
    indexSetTemplateDefaults: data,
  };
};

export default useIndexSetTemplateDefaults;
