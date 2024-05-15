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
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';

const fetchIndexSetTemplateDefaults: () => Promise<IndexSetTemplate['index_set_config']> = () => fetch('GET', qualifyUrl('/system/indices/index_sets/templates/default_config'));

const useIndexSetTemplateDefaults = () => {
  const { data, isLoading } = useQuery<IndexSetTemplate['index_set_config'], Error>(
    ['index-templates-defaults'],
    fetchIndexSetTemplateDefaults,
    {
      onError: (fetchError: Error) => {
        UserNotification.error(`Error fetching index default template: ${fetchError.message}`);
      },
      retry: 1,
    },
  );

  return {
    loadingIndexSetTemplateDefaults: isLoading,
    indexSetTemplateDefaults: data,
  };
};

export default useIndexSetTemplateDefaults;
