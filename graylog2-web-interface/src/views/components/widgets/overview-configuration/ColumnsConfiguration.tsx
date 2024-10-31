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
import { Field } from 'formik';
import * as Immutable from 'immutable';

import ColumnSelect from './ColumnSelect';
import SelectedColumnsList from './SelectedColumnsList';

type Props = {
  columnTitle: (column: string) => string,
  columns: Array<string>,
  createSelectPlaceholder?: string,
  displaySortableListOverlayInPortal?: boolean,
  menuPortalTarget?: HTMLElement,
  name: string,
  testPrefix?: string,
}

const ColumnsConfiguration = ({
  columnTitle,
  columns,
  createSelectPlaceholder = 'Add a column',
  displaySortableListOverlayInPortal = false,
  menuPortalTarget,
  name,
  testPrefix = '',
}: Props) => (
  <Field name={name}>
    {({ field: { value, onChange } }) => (
      <>
        <SelectedColumnsList testPrefix={testPrefix}
                             selectedColumns={value.toArray()}
                             columnTitle={columnTitle}
                             displayOverlayInPortal={displaySortableListOverlayInPortal}
                             onChange={(newColumns) => onChange({ target: { value: Immutable.OrderedSet(newColumns), name } })} />
        <ColumnSelect id="column-create-select"
                      columnTitle={columnTitle}
                      columns={columns}
                      onChange={(newColumn) => onChange({ target: { value: Immutable.OrderedSet([...value.toArray(), newColumn]), name } })}
                      clearable={false}
                      persistSelection={false}
                      name="column-create-select"
                      value={undefined}
                      menuPortalTarget={menuPortalTarget}
                      excludedColumns={value}
                      placeholder={createSelectPlaceholder}
                      ariaLabel={createSelectPlaceholder} />
      </>
    )}
  </Field>
);

export default ColumnsConfiguration;
