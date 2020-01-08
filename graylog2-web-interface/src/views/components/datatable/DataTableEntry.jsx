// @flow strict
import React from 'react';
import * as Immutable from 'immutable';
import { flatten, get } from 'lodash';

import Value from 'views/components/Value';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import type { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import type { CurrentViewType } from '../CustomPropTypes';
import CustomHighlighting from '../messagelist/CustomHighlighting';
import DecoratedValue from '../messagelist/decoration/DecoratedValue';
import EmptyValue from '../EmptyValue';

type Props = {
  columnPivots: Array<string>,
  columnPivotValues: Array<Array<string>>,
  currentView: CurrentViewType,
  fields: Immutable.Set<string>,
  item: { [string]: * },
  series: Array<Series>,
  types: FieldTypeMappingsList,
  valuePath: ValuePath,
};

const _c = (field, value, path) => ({ field, value, path });

const _column = (field: string, value: *, selectedQuery: string, idx: number, type: FieldType, valuePath: ValuePath) => (
  <td key={`${selectedQuery}-${field}=${value}-${idx}`}>
    <AdditionalContext.Provider value={{ valuePath }}>
      <CustomHighlighting field={field} value={value}>
        {value !== null && !value !== undefined ? <Value field={field} type={type} value={value} queryId={selectedQuery} render={DecoratedValue} /> : <EmptyValue />}
      </CustomHighlighting>
    </AdditionalContext.Provider>
  </td>
);

const fullValuePathForField = (fieldName, valuePath) => {
  const currentSeries = parseSeries(fieldName);
  return currentSeries && currentSeries.field ? [...valuePath, { _exists_: currentSeries.field }] : valuePath;
};

const columnNameToField = (column, series = []) => {
  const currentSeries = series.find(s => s.effectiveName === column);
  return currentSeries ? currentSeries.function : column;
};

const DataTableEntry = ({ columnPivots, currentView, fields, series, columnPivotValues, valuePath, item, types }: Props) => {
  const classes = 'message-group';
  const { activeQuery } = currentView;

  const fieldColumns = fields.toSeq().toJS().map((fieldName, i) => _c(
    fieldName,
    item[fieldName],
    fullValuePathForField(fieldName, valuePath).slice(0, i + 1),
  ));
  const columnPivotFields = flatten(columnPivotValues.map((columnPivotValueKeys) => {
    const translatedPath = flatten(columnPivotValueKeys.map((value, idx) => [columnPivots[idx], value]));
    const [k, v] = translatedPath;
    const parentValuePath = [...valuePath, { [k]: v }];
    return series.map(({ effectiveName, function: fn }) => {
      const fullPath = [].concat(translatedPath, [effectiveName]);
      const value = get(item, fullPath);
      return _c(effectiveName, value, fullValuePathForField(fn, parentValuePath));
    });
  }));

  const columns = flatten([fieldColumns, columnPivotFields]);
  return (
    <tbody className={classes}>
      <tr className="fields-row">
        {columns.map(({ field, value, path }, idx) => _column(field, value, activeQuery, idx, fieldTypeFor(columnNameToField(field, series), types), path.slice()))}
      </tr>
    </tbody>
  );
};

export default DataTableEntry;
