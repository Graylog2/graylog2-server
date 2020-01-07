// @flow strict
import React from 'react';
import { flatten, get, isEqual, last } from 'lodash';

import Field from 'views/components/Field';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Value from 'views/components/Value';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';

import styles from './DataTable.css';

const _headerField = (activeQuery: string, fields, field: string, prefix: (string | number) = '', span: number = 1, title: string = field) => (
  <th key={`${prefix}${field}`} colSpan={span} className={styles.leftAligned}>
    <Field name={field} queryId={activeQuery} type={fieldTypeFor(field, fields)}>{title}</Field>
  </th>
);

const _headerFieldForValue = (activeQuery: string, field, value, span = 1, prefix = '') => (
  <th key={`${prefix}${field}-${value}`} colSpan={span} className={styles.leftAligned}>
    <Value field={field} value={value} queryId={activeQuery} type={FieldType.Unknown}>{value}</Value>
  </th>
);

const _spacer = (idx, span = 1) => <th colSpan={span} key={`spacer-${idx}`} className={styles.leftAligned} />;

const columnPivotFieldsHeaders = (activeQuery, columnPivots, actualColumnPivotValues, series, offset = 1) => {
  return columnPivots.map((columnPivot, idx) => {
    const actualValues = actualColumnPivotValues.map(key => ({ path: key.slice(0, idx).join('-'), key: key[idx] || '', count: 1 }));
    const actualValuesWithoutDuplicates = actualValues.reduce((prev, cur) => {
      const lastKey = get(last(prev), 'key');
      const lastPath = get(last(prev), 'path');
      if (lastKey === cur.key && isEqual(lastPath, cur.path)) {
        const lastItem = last(prev);
        const remainder = prev.slice(0, -1);
        const newLastItem = Object.assign({}, lastItem, { count: lastItem.count + 1 });
        return [].concat(remainder, [newLastItem]);
      }
      return [].concat(prev, [cur]);
    }, []);
    return (
      <tr key={`header-table-row-${columnPivot}`}>
        {offset > 0 && _spacer(1, offset)}
        {actualValuesWithoutDuplicates.map(value => _headerFieldForValue(activeQuery, columnPivot, value.key, value.count * series.length, value.path))}
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
  const rowFieldNames = rowPivots.map(pivot => pivot.field);
  const columnFieldNames = columnPivots.map(pivot => pivot.field);
  const headerField = (field, prefix = '', span: number = 1, title = field) => _headerField(activeQuery, fields, field, prefix, span, title);
  const rowPivotFields = rowFieldNames.map(fieldName => headerField(fieldName));
  const seriesFields = series.map(s => headerField(s.function, '', 1, s.effectiveName));
  const columnPivotFields = flatten(actualColumnPivotFields.map(key => series.map(s => headerField(s.function, key.join('-'), 1, s.effectiveName))));
  const offset = rollup ? rowFieldNames.length + series.length : rowFieldNames.length;

  return (
    <React.Fragment>
      {columnPivotFieldsHeaders(activeQuery, columnFieldNames, actualColumnPivotFields, series, offset)}
      <tr>
        {rowPivotFields}
        {rollup && seriesFields}
        {columnPivotFields}
      </tr>
    </React.Fragment>
  );
};

export default Headers;
