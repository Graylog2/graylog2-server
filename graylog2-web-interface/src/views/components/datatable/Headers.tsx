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
import React from 'react';
import { flatten, get, isEqual, last } from 'lodash';
import styled, { css } from 'styled-components';

import Field from 'views/components/Field';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Value from 'views/components/Value';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';

import styles from './DataTable.css';

const StyledTh = styled.th(({ isNumeric }: { isNumeric: boolean }) => css`
  ${isNumeric ? 'text-align: right' : ''}
`);

const _headerField = (activeQuery: string, fields, field: string, prefix: (string | number) = '', span: number = 1, title: string = field) => {
  const type = fieldTypeFor(field, fields);

  return (
    <StyledTh isNumeric={type.isNumeric()} key={`${prefix}${field}`} colSpan={span} className={styles.leftAligned}>
      <Field name={field} queryId={activeQuery} type={type}>{title}</Field>
    </StyledTh>
  );
};

const _headerFieldForValue = (activeQuery: string, field, value, span = 1, prefix = '') => (
  <th key={`${prefix}${field}-${value}`} colSpan={span} className={styles.leftAligned}>
    <Value field={field} value={value} queryId={activeQuery} type={FieldType.Unknown}>{value}</Value>
  </th>
);

// eslint-disable-next-line jsx-a11y/control-has-associated-label
const _spacer = (idx, span = 1) => <th colSpan={span} key={`spacer-${idx}`} className={styles.leftAligned} />;

const columnPivotFieldsHeaders = (activeQuery: string, columnPivots: string[], actualColumnPivotValues: any[], series: Series[], offset = 1) => {
  return columnPivots.map((columnPivot, idx) => {
    const actualValues = actualColumnPivotValues.map((key) => ({ path: key.slice(0, idx).join('-'), key: key[idx] || '', count: 1 }));
    const actualValuesWithoutDuplicates = actualValues.reduce((prev, cur) => {
      const lastKey = get(last(prev), 'key');
      const lastPath = get(last(prev), 'path');

      if (lastKey === cur.key && isEqual(lastPath, cur.path)) {
        const lastItem = last(prev);
        const remainder = prev.slice(0, -1);
        const newLastItem = { ...lastItem, count: lastItem.count + 1 };

        return [].concat(remainder, [newLastItem]);
      }

      return [].concat(prev, [cur]);
    }, []);

    return (
      <tr key={`header-table-row-${columnPivot}`}>
        {offset > 0 && _spacer(1, offset)}
        {actualValuesWithoutDuplicates.map((value) => _headerFieldForValue(activeQuery, columnPivot, value.key, value.count * series.length, value.path))}
      </tr>
    );
  });
};

type Props = {
  activeQuery: string,
  columnPivots: Array<Pivot>,
  rowPivots: Array<Pivot>,
  series: Array<Series>,
  rollup: boolean,
  actualColumnPivotFields: Array<Array<string>>,
  fields: FieldTypeMappingsList,
};

const Headers = ({ activeQuery, columnPivots, fields, rowPivots, series, rollup, actualColumnPivotFields }: Props) => {
  const rowFieldNames = rowPivots.map((pivot) => pivot.field);
  const columnFieldNames = columnPivots.map((pivot) => pivot.field);
  const headerField = (field, prefix = '', span: number = 1, title = field) => _headerField(activeQuery, fields, field, prefix, span, title);
  const rowPivotFields = rowFieldNames.map((fieldName) => headerField(fieldName));
  const seriesFields = series.map((s) => headerField(s.function, '', 1, s.effectiveName));
  const columnPivotFields = flatten(actualColumnPivotFields.map((key) => series.map((s) => headerField(s.function, key.join('-'), 1, s.effectiveName))));
  const offset = rollup ? rowFieldNames.length + series.length : rowFieldNames.length;

  return (
    <>
      {columnPivotFieldsHeaders(activeQuery, columnFieldNames, actualColumnPivotFields, series, offset)}
      <tr>
        {rowPivotFields}
        {rollup && seriesFields}
        {columnPivotFields}
      </tr>
    </>
  );
};

export default Headers;
