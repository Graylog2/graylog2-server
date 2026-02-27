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
import { useParams } from 'react-router-dom';

import { Spinner } from 'components/common';
import {
  useFetchLookupTable,
  useFetchCache,
  useFetchDataAdapter,
} from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { ErrorsProvider } from 'components/lookup-tables/contexts/ErrorsContext';

import LookupTableShow from './lookup-table-show';

function LookupTableView() {
  const { lutIdOrName } = useParams<{ lutIdOrName: string }>();
  const { lookupTable, loadingLookupTable } = useFetchLookupTable(lutIdOrName);
  const { cache, loadingCache } = useFetchCache(lookupTable?.cache_id);
  const { dataAdapter, loadingDataAdapter } = useFetchDataAdapter(lookupTable?.data_adapter_id);

  if (loadingLookupTable || loadingCache || loadingDataAdapter) return <Spinner text="Loading lookup table details" />;

  return (
    <ErrorsProvider>
      <LookupTableShow table={lookupTable} cache={cache} dataAdapter={dataAdapter} />
    </ErrorsProvider>
  );
}

export default LookupTableView;
