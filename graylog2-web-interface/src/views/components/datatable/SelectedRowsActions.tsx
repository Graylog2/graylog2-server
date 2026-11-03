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
import { useId } from 'react';
import styled, { css } from 'styled-components';

import { DropdownButton, Menu, MenuItem } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { addValuesToQuery, excludeValuesFromQuery } from 'views/logic/valueactions/MultipleValuesQueryHandler';

import type { FieldValues } from './rowSelection';

const Container = styled.div(
  ({ theme }) => css`
    grid-row: 1;
    grid-column: 1;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: ${theme.spacings.sm};
    padding: ${theme.spacings.xs} 0;
  `,
);

type FieldsSubmenuProps = {
  title: string;
  fieldValues: Array<FieldValues>;
  onSelect: (fieldValues: FieldValues) => void;
};

const FieldsSubmenu = ({ title, fieldValues, onSelect }: FieldsSubmenuProps) => (
  <Menu.Sub>
    <Menu.Sub.Target>
      <Menu.Sub.Item>{title}</Menu.Sub.Item>
    </Menu.Sub.Target>
    <Menu.Sub.Dropdown>
      {fieldValues.map((curFieldValues) => (
        <MenuItem key={curFieldValues.field} onClick={() => onSelect(curFieldValues)}>
          {curFieldValues.field}
        </MenuItem>
      ))}
    </Menu.Sub.Dropdown>
  </Menu.Sub>
);

type Props = {
  selectedRowsCount: number;
  fieldValues: Array<FieldValues>;
  onClearSelection: () => void;
};

const SelectedRowsActions = ({ selectedRowsCount, fieldValues, onClearSelection }: Props) => {
  const dropdownId = useId();
  const dispatch = useViewsDispatch();
  const queryId = useActiveQueryId();

  const onAddToQuery = ({ field, values, type }: FieldValues) =>
    dispatch(addValuesToQuery({ queryId, field, values, type })).then(onClearSelection);
  const onExclude = ({ field, values, type }: FieldValues) =>
    dispatch(excludeValuesFromQuery({ queryId, field, values, type })).then(onClearSelection);

  return (
    <Container>
      <DropdownButton bsSize="xsmall" title="Bulk actions" id={`selected-rows-actions-${dropdownId}`}>
        <FieldsSubmenu title="Add to query" fieldValues={fieldValues} onSelect={onAddToQuery} />
        <FieldsSubmenu title="Exclude from results" fieldValues={fieldValues} onSelect={onExclude} />
        <MenuItem divider />
        <MenuItem onClick={onClearSelection}>Cancel selection</MenuItem>
      </DropdownButton>
      <span>
        {selectedRowsCount} {StringUtils.pluralize(selectedRowsCount, 'row', 'rows')} selected
      </span>
    </Container>
  );
};

export default SelectedRowsActions;
