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
import * as React from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import usePluginEntities from 'hooks/usePluginEntities';
import { Select } from 'components/common';
import { Input } from 'components/bootstrap';
import { useFetchCacheTypes } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableCache } from 'logic/lookup-tables/types';

type Props = {
  cacheConfigType: string;
  onCacheChange: (cache: LookupTableCache) => void;
};

function CacheTypeSelect({ cacheConfigType, onCacheChange }: Props) {
  const { types, fetchingCacheTypes } = useFetchCacheTypes();
  const plugins = usePluginEntities('lookupTableAdapters');
  const cachePlugins = Object.fromEntries(plugins.map((p) => [p.type, p]));

  const sortedCaches = React.useMemo(() => {
    if (!fetchingCacheTypes) {
      return Object.values(types)
        .map(({ type }) => ({ value: type, label: cachePlugins[type].displayName }))
        .sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));
    }

    return [];
  }, [fetchingCacheTypes, types, cachePlugins]);

  const handleTypeSelect = React.useCallback(
    (cacheType: string) => {
      const defaultConfig = { ...types[cacheType].default_config };

      onCacheChange({
        id: null,
        title: '',
        name: '',
        description: '',
        config: defaultConfig,
      });
    },
    [onCacheChange, types],
  );

  return (
    <Input id="cache-type-select" label="Cache Type" required help="The type of cache to configure.">
      <Select
        placeholder="Select Cache Type"
        clearable={false}
        options={sortedCaches}
        onChange={handleTypeSelect}
        value={cacheConfigType}
      />
    </Input>
  );
}

export default CacheTypeSelect;
