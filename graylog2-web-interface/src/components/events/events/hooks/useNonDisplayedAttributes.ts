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

import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import type { Attribute } from 'stores/PaginationTypes';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';

const useNonDisplayedAttributes = (defaultLayout) => {
  const { attributes } = useTableFetchContext();
  const { layoutConfig: { displayedAttributes }, isInitialLoading } = useTableLayout(defaultLayout);

  return useMemo<Array<Attribute>>(() => {
    if (isInitialLoading) return [];

    const displayedAttributesSet = new Set(displayedAttributes);

    return attributes.filter(({ id }) => !displayedAttributesSet.has(id));
  }, [attributes, displayedAttributes, isInitialLoading]);
};

export default useNonDisplayedAttributes;
