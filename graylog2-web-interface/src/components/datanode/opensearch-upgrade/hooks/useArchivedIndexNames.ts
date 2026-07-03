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

// Archiving lives in the enterprise Archive plugin, whose `@graylog/server-api` stub is absent in CI. Call the
// catalog endpoint directly — like archiveAndDeleteIndex in outdatedIndexActions — to stay CI-safe.
const ARCHIVE_CATALOG_URL = '/plugins/org.graylog.plugins.archive/cluster/archives/catalog';

type CatalogPage = {
  archives?: Array<{ index_name?: string }>;
};

const EMPTY_SET: Set<string> = new Set();

// Match only the given index names exactly: `index:=<name>` forces the EQUALS operator (the field otherwise
// defaults to a regex match), and repeating the same field ORs the values server-side. Index names never
// contain whitespace, so they need no escaping for the query splitter.
const buildIndexNameQuery = (indexNames: Array<string>) => indexNames.map((name) => `index:=${name}`).join(' ');

const fetchArchivedIndexNames = async (indexNames: Array<string>): Promise<Set<string>> => {
  const query = `query=${encodeURIComponent(buildIndexNameQuery(indexNames))}&per_page=${ARCHIVE_CATALOG_LOOKUP_MAX_ENTRIES}`;

  const page = (await fetch('GET', qualifyUrl(`${ARCHIVE_CATALOG_URL}?${query}`))) as CatalogPage;

  return new Set((page.archives ?? []).map((entry) => entry.index_name).filter((name): name is string => !!name));
};

/**
 * Returns the subset of `indexNames` that already have an archive in the catalog. Unlike the localStorage-backed
 * pending-action tracking, this is the durable, cluster-wide source of truth: it survives reloads and other
 * browsers, and it also reflects archives created outside the upgrade page (e.g. by retention). Used to flag the
 * "archived but delete skipped" case and to avoid re-offering "Archive and delete" for already-archived indices.
 *
 * Only queries while `enabled` (archiving available) and there is at least one index name to look up.
 */
const useArchivedIndexNames = (indexNames: Array<string>, enabled: boolean): Set<string> => {
  // Sort so the query key is stable regardless of the incoming order, keeping the cache hit rate high.
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
