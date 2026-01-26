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
import styled from 'styled-components';
import { TextInput, SegmentedControl, Select, Group } from '@mantine/core';

type StatusFilter = 'all' | 'online' | 'offline';

type Props = {
  searchValue: string;
  onSearchChange: (value: string) => void;
  statusFilter: StatusFilter;
  onStatusFilterChange: (value: StatusFilter) => void;
  fleetFilter: string | null;
  onFleetFilterChange: (value: string | null) => void;
  fleetOptions: Array<{ value: string; label: string }>;
};

const FilterContainer = styled(Group)`
  margin-bottom: 1rem;
`;

const FilterBar = ({
  searchValue,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  fleetFilter,
  onFleetFilterChange,
  fleetOptions,
}: Props) => (
  <FilterContainer>
    <TextInput
      placeholder="Filter by hostname..."
      value={searchValue}
      onChange={(e) => onSearchChange(e.target.value)}
      style={{ flex: 1, maxWidth: 300 }}
    />
    <SegmentedControl
      value={statusFilter}
      onChange={(v) => onStatusFilterChange(v as StatusFilter)}
      data={[
        { value: 'all', label: 'All' },
        { value: 'online', label: 'Online' },
        { value: 'offline', label: 'Offline' },
      ]}
    />
    <Select
      placeholder="All Fleets"
      data={[{ value: '', label: 'All Fleets' }, ...fleetOptions]}
      value={fleetFilter || ''}
      onChange={(v) => onFleetFilterChange(v || null)}
      clearable
      style={{ minWidth: 150 }}
    />
  </FilterContainer>
);

export default FilterBar;
