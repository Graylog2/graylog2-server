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
import { LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';
import { useStore } from 'stores/connect';
import { useFetchCaches } from 'components/lookup-tables/hooks/useLookupTablesAPI';

type Props = {
  children: React.ReactElement;
};

const CachesContainer = ({ children }: Props) => {
  const { caches, pagination } = useStore(LookupTableCachesStore);
  const { fetchPaginatedCaches } = useFetchCaches();

  useEffect(() => {
    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    fetchPaginatedCaches({ page: 1, pageSize: 10000, query: null, sort: { attributeId: 'name', direction: 'asc' } });
  }, []);

  if (!caches) {
    return <Spinner />;
  }

  const childrenWithProps = React.Children.map(children, (child) => React.cloneElement(child, { caches, pagination }));

  return <div>{childrenWithProps}</div>;
};

export default CachesContainer;
