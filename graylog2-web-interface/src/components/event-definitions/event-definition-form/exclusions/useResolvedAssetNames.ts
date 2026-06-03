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
import { qualifyUrl } from 'util/URLUtils';

type AssetDetails = { id: string; name?: string };
type AssetsByIdsResponse = Record<string, AssetDetails>;

const resolveAssetsByIds = async (ids: string[]): Promise<AssetsByIdsResponse> => {
  const response: AssetsByIdsResponse | undefined = await fetch(
    'POST',
    qualifyUrl('/plugins/org.graylog.plugins.securityapp.asset/assets/byIds'),
    { asset_ids: ids },
  );

  return response ?? {};
};

const useResolvedAssetNames = (ids: ReadonlyArray<string>): Record<string, string> => {
  const sortedIds = useMemo(() => [...ids].sort(), [ids]);
  const { data } = useQuery({
    queryKey: ['assets', 'byIds', sortedIds],
    queryFn: () => resolveAssetsByIds(sortedIds),
    enabled: sortedIds.length > 0,
    retry: false,
    notifyOnChangeProps: ['data'],
  });

  return useMemo(() => {
    const out: Record<string, string> = {};

    if (data) {
      Object.entries(data).forEach(([id, details]) => {
        if (details?.name) {
          out[id] = details.name;
        }
      });
    }

    return out;
  }, [data]);
};

export default useResolvedAssetNames;
