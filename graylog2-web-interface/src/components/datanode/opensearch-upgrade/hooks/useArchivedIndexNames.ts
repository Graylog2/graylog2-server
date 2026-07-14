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
import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';
import { qualifyUrl } from 'util/URLUtils';

import { ARCHIVE_CATALOG_LOOKUP_MAX_ENTRIES } from '../constants';

// Fetched raw because the enterprise Archive plugin's `@graylog/server-api` stub is absent in CI.
const ARCHIVE_CATALOG_URL = '/plugins/org.graylog.plugins.archive/cluster/archives/catalog';

type CatalogPage = {
  archives?: Array<{ index_name?: string }>;
};

const EMPTY_SET: Set<string> = new Set();

const buildIndexNameQuery = (indexNames: Array<string>) => indexNames.map((name) => `index:=${name}`).join(' ');

const fetchArchivedIndexNames = async (indexNames: Array<string>): Promise<Set<string>> => {
  const query = `query=${encodeURIComponent(buildIndexNameQuery(indexNames))}&per_page=${ARCHIVE_CATALOG_LOOKUP_MAX_ENTRIES}`;

  const page = (await fetch('GET', qualifyUrl(`${ARCHIVE_CATALOG_URL}?${query}`))) as CatalogPage;

  return new Set((page.archives ?? []).map((entry) => entry.index_name).filter((name): name is string => !!name));
};

const useArchivedIndexNames = (indexNames: Array<string>, enabled: boolean): Set<string> => {
  const sortedIndexNames = useMemo(() => [...indexNames].sort(), [indexNames]);
  const isEnabled = enabled && sortedIndexNames.length > 0;

  const { data } = useQuery({
    queryKey: ['opensearch-upgrade', 'archived-index-names', sortedIndexNames],
    queryFn: () =>
      defaultOnError(
        fetchArchivedIndexNames(sortedIndexNames),
        'Loading archived indices failed',
        'Could not determine which indices are already archived',
      ),
    enabled: isEnabled,
  });

  return data ?? EMPTY_SET;
};

export default useArchivedIndexNames;
