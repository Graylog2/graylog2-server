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
import styled from 'styled-components';
import * as React from 'react';
import { useTranslation } from 'react-i18next';
import type { Column, Table } from '@tanstack/react-table';

import { defaultCompare } from 'logic/DefaultCompare';
import { Checkbox, DropdownButton, MenuItem, DeleteMenuItem } from 'components/bootstrap';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';
import { Icon } from 'components/common';

const StyledDropdownButton = styled(DropdownButton)`
  ~ .dropdown-menu {
    min-width: auto;
    max-width: 180px;
  }
`;

const ColumnCheckbox = styled(Checkbox)`
  &.checkbox {
    margin: 0 5px 0 0;

    label {
      display: flex;
      align-items: center;
      padding: 0;

      input {
        margin: 0;
        position: relative;
      }
    }
  }
`;

const ListItemInner = styled.div`
  display: flex;
`;

const ColumnTitle = styled(TextOverflowEllipsis)`
  display: inline;
`;

const colLabel = <Entity extends EntityBase>(column: Column<Entity>) =>
  (column.columnDef.meta as ColumnMetaContext<Entity>)?.label ?? column.id;

const ColumnListItem = <Entity extends EntityBase>({ column }: { column: Column<Entity> }) => {
  const isSelected = column.getIsVisible();
  const toggleVisibility = () => column.toggleVisibility();
  const label = colLabel<Entity>(column);
  const { t } = useTranslation('common');

  return (
    <MenuItem
      onSelect={toggleVisibility}
      title={
        isSelected
          ? t('entity_table.hide_column_named', { column: label })
          : t('entity_table.show_column', { column: label })
      }>
      <ListItemInner>
        <ColumnCheckbox checked={isSelected} onChange={toggleVisibility} />
        <ColumnTitle>{label}</ColumnTitle>
      </ListItemInner>
    </MenuItem>
  );
};

type Props<Entity> = {
  onResetLayoutPreferences: () => void;
  table: Table<Entity>;
  hasColumnLayoutPreferences: boolean;
};

const ColumnsVisibilitySelect = <Entity extends EntityBase>({
  table,
  onResetLayoutPreferences,
  hasColumnLayoutPreferences,
}: Props<Entity>) => {
  const { t } = useTranslation('common');

  return (
    <StyledDropdownButton
      title={t('entity_table.columns')}
      withinPortal
      bsSize="small"
      pullRight
      aria-label={t('entity_table.configure_columns')}
      id="columns-visibility-select"
      bsStyle="default"
      closeOnItemClick={false}>
      {table
        .getAllLeafColumns()
        .filter((column) => column.getCanHide())
        .sort((col1, col2) => defaultCompare(colLabel<Entity>(col1), colLabel<Entity>(col2)))
        .map((column) => (
          <ColumnListItem<Entity> column={column} key={column.id} />
        ))}
      <MenuItem divider />
      <DeleteMenuItem disabled={!hasColumnLayoutPreferences} onSelect={onResetLayoutPreferences} closeMenuOnClick>
        <Icon name="reopen_window" /> {t('entity_table.reset_columns')}
      </DeleteMenuItem>
    </StyledDropdownButton>
  );
};

export default ColumnsVisibilitySelect;
