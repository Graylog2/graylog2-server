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

import { Menu, MenuItem } from 'components/bootstrap';
import BulkActionsRow from 'components/common/EntityDataTable/BulkActionsRow';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import AddToQueryHandler from 'views/logic/valueactions/AddToQueryHandler';
import ExcludeFromQueryHandler from 'views/logic/valueactions/ExcludeFromQueryHandler';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import type { QueryValue } from 'views/logic/valueactions/ValueActionQueryHelper';

import { selectedFieldValues } from './rowSelection';

const StyledBulkActionsRow = styled(BulkActionsRow)`
  padding-bottom: 10px;
`;

type FieldValues = { field: string; type: FieldType; values: Array<QueryValue> };

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
  selectableFields: Array<string>;
  types: FieldTypeMappingsList;
};

const SelectedRowsActions = ({ selectableFields, types }: Props) => {
  const dispatch = useViewsDispatch();
  const queryId = useActiveQueryId();
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const fieldValues = selectableFields.map((field) => ({
    field,
    type: fieldTypeFor(field, types),
    values: selectedFieldValues(selectedEntities, field),
  }));
  const onClearSelection = () => setSelectedEntities([]);

  const onAddToQuery = ({ field, values, type }: FieldValues) =>
    dispatch(AddToQueryHandler({ queryId, field, value: values, type })).then(onClearSelection);
  const onExclude = ({ field, values, type }: FieldValues) =>
    dispatch(ExcludeFromQueryHandler({ queryId, field, value: values, type })).then(onClearSelection);

  return (
    <StyledBulkActionsRow
      bulkActions={
        <BulkActionsDropdown bsSize="xs">
          <FieldsSubmenu title="Add to query" fieldValues={fieldValues} onSelect={onAddToQuery} />
          <FieldsSubmenu title="Exclude from results" fieldValues={fieldValues} onSelect={onExclude} />
        </BulkActionsDropdown>
      }
    />
  );
};

export default SelectedRowsActions;
