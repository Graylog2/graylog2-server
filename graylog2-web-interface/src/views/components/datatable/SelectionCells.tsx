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

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';

import TableHeaderCell from './TableHeaderCell';

const SelectionHeader = styled(TableHeaderCell)`
  && {
    width: 1px;
  }
`;

const SelectionData = styled.td`
  && {
    width: 1px;
  }
`;

export type SelectAllProps = {
  checked: boolean;
  indeterminate: boolean;
  onChange: () => void;
};

export const SelectAllHeaderCell = ({ checked, indeterminate, onChange }: SelectAllProps) => {
  const label = checked ? 'Deselect all rows' : 'Select all rows';

  return (
    <SelectionHeader>
      <RowCheckbox checked={checked} indeterminate={indeterminate} onChange={onChange} title={label} aria-label={label} />
    </SelectionHeader>
  );
};

export type SelectRowProps = {
  checked: boolean;
  onChange: () => void;
  label: string;
};

export const SelectRowCell = ({ checked, onChange, label }: SelectRowProps) => (
  <SelectionData>
    <RowCheckbox checked={checked} onChange={onChange} title={label} aria-label={label} />
  </SelectionData>
);
