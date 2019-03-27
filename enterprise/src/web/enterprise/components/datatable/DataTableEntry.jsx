// @flow strict
import React from 'react';
import * as Immutable from 'immutable';
import { flatten, get } from 'lodash';

import Value from 'enterprise/components/Value';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'enterprise/logic/ActionContext';
import type { ValuePath } from 'enterprise/logic/valueactions/ValueActionHandler';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'enterprise/stores/FieldTypesStore';
import type { CurrentViewType } from '../CustomPropTypes';

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
      {value && <Value field={field} type={type} value={value} queryId={selectedQuery} />}
    </AdditionalContext.Provider>
  </td>
);

const _fieldTypeFor = (field: string, types: FieldTypeMappingsList) => {
  const fieldType = types.find(f => f.name === field);
  return fieldType ? fieldType.type : FieldType.Unknown;
};

const DataTableEntry = ({ columnPivots, currentView, fields, series, columnPivotValues, valuePath, item, types }: Props) => {
  const classes = 'message-group';
  const { activeQuery } = currentView;

  const fieldColumns = fields.toSeq().toJS().map((fieldName, i) => _c(fieldName, item[fieldName], valuePath.slice(0, i + 1)));
  const columnPivotFields = flatten(columnPivotValues.map((columnPivotValueKeys) => {
    const translatedPath = flatten(columnPivotValueKeys.map((value, idx) => [columnPivots[idx], value]));
    const [k, v] = translatedPath;
    const parentValuePath = [...valuePath, { [k]: v }];
    return series.map(({ effectiveName }) => {
      const fullPath = [].concat(translatedPath, [effectiveName]);
      const value = get(item, fullPath);
      return _c(effectiveName, value, parentValuePath);
    });
  }));

  const columns = flatten([fieldColumns, columnPivotFields]);
  return (
    <tbody className={classes}>
      <tr className="fields-row">
        {columns.map(({ field, value, path }, idx) => _column(field, value, activeQuery, idx, _fieldTypeFor(field, types), path.slice()))}
      </tr>
    </tbody>
  );
};

export default DataTableEntry;
