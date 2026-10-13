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
import { OrderedMap } from 'immutable';

import useProductName from 'brand-customization/useProductName';
import { Button, ButtonGroup } from 'components/bootstrap';
import { Tooltip } from 'components/common';
import useTableFilterContext from 'components/common/PaginatedEntityTable/useTableFilterContext';

import { MANAGED_ATTRIBUTE, managedCounts } from './indexList';
import useIndexOverview from './useIndexOverview';

type Option = 'all' | 'managed' | 'unmanaged';

type ManagedOption = { value: Option; title: string; description: (productName: string) => string; filter?: string };

const OPTIONS: Array<ManagedOption> = [
  { value: 'all', title: 'All', description: () => 'All indices in the cluster.' },
  {
    value: 'managed',
    title: 'Managed',
    description: (productName) => `Indices of a ${productName} index set.`,
    filter: 'true',
  },
  {
    value: 'unmanaged',
    title: 'Unmanaged',
    description: (productName) =>
      `Indices outside any ${productName} index set, e.g. created by OpenSearch or other tools.`,
    filter: 'false',
  },
];

// Narrows the list to indices Graylog manages, or the others, through the table's "Managed by Graylog" filter.
const IndexManagedButtons = () => {
  const { searchParams, onChangeFilters } = useTableFilterContext();
  const { indices } = useIndexOverview();
  const productName = useProductName();
  const counts = managedCounts(indices);
  const filters = searchParams.filters ?? OrderedMap<string, Array<string>>();
  const selected = filters.get(MANAGED_ATTRIBUTE) ?? [];
  const active = OPTIONS.find(({ filter }) =>
    filter ? selected.length === 1 && selected[0] === filter : selected.length === 0,
  )?.value;

  const onSelect = (filter: string | undefined) =>
    onChangeFilters(filter ? filters.set(MANAGED_ATTRIBUTE, [filter]) : filters.delete(MANAGED_ATTRIBUTE));

  return (
    <ButtonGroup>
      {OPTIONS.map(({ value, title, description, filter }) => (
        <Tooltip key={value} withArrow position="bottom" label={description(productName)}>
          <Button bsSize="small" active={active === value} onClick={() => onSelect(filter)}>
            {title} <b>{counts[value]}</b>
          </Button>
        </Tooltip>
      ))}
    </ButtonGroup>
  );
};

export default IndexManagedButtons;
