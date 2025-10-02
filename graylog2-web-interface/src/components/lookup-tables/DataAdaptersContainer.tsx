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
import { useEffect } from 'react';

import { Spinner } from 'components/common';
import { LookupTableDataAdaptersStore } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import { useStore } from 'stores/connect';
import { useFetchDataAdapters } from 'components/lookup-tables/hooks/useLookupTablesAPI';

type Props = {
  children: React.ReactElement;
  dataAdapter?: string;
};

const DataAdaptersContainer = ({ children, dataAdapter = '' }: Props) => {
  const { dataAdapters, pagination } = useStore(LookupTableDataAdaptersStore);
  const { fetchPaginatedDataAdapters } = useFetchDataAdapters();

  useEffect(() => {
    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    fetchPaginatedDataAdapters({
      page: 1,
      pageSize: 10000,
      query: null,
      sort: { attributeId: 'name', direction: 'asc' },
    });
  }, [dataAdapter, fetchPaginatedDataAdapters]);

  if (!dataAdapters) {
    return <Spinner />;
  }

  const childrenWithProps = React.Children.map(children, (child) =>
    React.cloneElement(child, { dataAdapters, pagination }),
  );

  return <div>{childrenWithProps}</div>;
};

export default DataAdaptersContainer;
