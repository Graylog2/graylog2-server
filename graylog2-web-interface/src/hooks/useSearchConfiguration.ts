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

import type { SearchesConfig } from 'components/search/SearchConfig';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

const fetchSearchesConfiguration = () => fetch('GET', qualifyUrl(`${ApiRoutes.ClusterConfigResource.config().url}/org.graylog2.indexer.searches.SearchesClusterConfig`));

type Result = { config: SearchesConfig, refresh: () => void };

const useSearchConfiguration = (): Result => {
  const { data: config, refetch } = useQuery(['cluster.config', 'org.graylog2.indexer.searches.SearchesClusterConfig'], fetchSearchesConfiguration);

  return { config, refresh: refetch };
};

export default useSearchConfiguration;
