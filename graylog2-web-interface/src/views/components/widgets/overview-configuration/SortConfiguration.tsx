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
import { useMemo } from 'react';
import { Field, useFormikContext } from 'formik';

import { Select } from 'components/common';
import { Input } from 'components/bootstrap';
import { defaultCompare } from 'logic/DefaultCompare';

const Container = styled.div`
  display: flex;
  flex-direction: column;
`;

type Props = {
  columnTitle?: (column: string) => string,
  columns: Array<string>,
  directionTitle?: (direction: string) => string,
  directions: Array<string>,
  name: string,
}

const SortConfiguration = ({ name: attributeName, directions, columns, columnTitle = (column: string) => column, directionTitle = (direction: string) => direction }: Props) => {
  const { values } = useFormikContext();
  const columnOptions = useMemo(() => (
    columns
      .map((col) => ({ value: col, label: columnTitle(col) }))
      .sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2))
  ), [columnTitle, columns]);

  const directionOptions = useMemo(() => (
    directions
      .map((col) => ({ value: col, label: directionTitle(col) }))
      .sort(({ label: label1 }, { label: label2 }) => defaultCompare(label1, label2))
  ), [directionTitle, directions]);

  const isUnknownColumn = values[attributeName].field && !columns.includes(values[attributeName].field);

  return (
    <Container>
      <Field name={`${attributeName}.field`}>
        {({ field: { value, onChange, name } }) => (
          <Input id="sort-configuration-column"
                 label="Column"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select id="sort-configuration-column-select"
                    placeholder="Select a column"
                    options={columnOptions}
                    matchProp="label"
                    menuPortalTarget={document.body}
                    clearable={false}
                    size="small"
                    onChange={(newColumn) => onChange({ target: { value: newColumn, name } })}
                    value={isUnknownColumn ? 'Unknown' : value} />
          </Input>
        )}
      </Field>

      <Field name={`${attributeName}.direction`}>
        {({ field: { value, onChange, name } }) => (
          <Input id="sort-configuration-direction"
                 label="Direction"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select id="sort-configuration-direction-select"
                    placeholder="Select a direction"
                    options={directionOptions}
                    menuPortalTarget={document.body}
                    matchProp="label"
                    clearable={false}
                    size="small"
                    onChange={(newDirection) => onChange({ target: { value: newDirection, name } })}
                    value={value} />
          </Input>
        )}
      </Field>
    </Container>
  );
};

export default SortConfiguration;
