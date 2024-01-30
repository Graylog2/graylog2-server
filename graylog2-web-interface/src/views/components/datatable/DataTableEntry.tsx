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
import { useMemo } from 'react';
import type * as Immutable from 'immutable';
import flatten from 'lodash/flatten';
import get from 'lodash/get';

import Value from 'views/components/Value';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import type { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import type Series from 'views/logic/aggregationbuilder/Series';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

import TableDataCell from './TableDataCell';

import CustomHighlighting from '../highlighting/CustomHighlighting';
import DecoratedValue from '../messagelist/decoration/DecoratedValue';

type Field = {
  field: string,
  source: string,
};
type Props = {
  columnPivots: Array<string>,
  columnPivotValues: Array<Array<string>>,
  fields: Immutable.Set<Field>,
  item: { [key: string]: any },
  series: Array<Series>,
  types: FieldTypeMappingsList,
  valuePath: ValuePath,
};

const _c = (field, value, path, source) => ({ field, value, path, source });

type ColumnProps = { field: string, value: any, type: FieldType, valuePath: ValuePath, source: string | undefined | null };

const flattenValuePath = (valuePath: ValuePath) => valuePath.flatMap((path) => Object.entries(path))
  .map(([key, value]) => `${key}:${value}`)
  .join('-');

const Column = ({ field, value, type, valuePath, source }: ColumnProps) => {
  const additionalContextValue = useMemo(() => ({ valuePath }), [valuePath]);

  return (
    <TableDataCell $isNumeric={type.isNumeric()} data-testid={`value-cell-${flattenValuePath(valuePath)}-${field}`}>
      <AdditionalContext.Provider value={additionalContextValue}>
        <CustomHighlighting field={source ?? field} value={value}>
          {value !== null && value !== undefined
            ? (
              <Value field={source ?? field}
                     type={type}
                     value={value}
                     render={DecoratedValue} />
            ) : null}
        </CustomHighlighting>
      </AdditionalContext.Provider>
    </TableDataCell>
  );
};

const fullValuePathForField = (fieldName, valuePath) => {
  const currentSeries = parseSeries(fieldName);

  return currentSeries && currentSeries.field ? [...valuePath, { _exists_: currentSeries.field }] : valuePath;
};

const columnNameToField = (column, series = []) => {
  const currentSeries = series.find((s) => s.effectiveName === column);

  return currentSeries ? currentSeries.function : column;
};

const DataTableEntry = ({ columnPivots, fields, series, columnPivotValues, valuePath, item, types }: Props) => {
  const classes = 'message-group';
  const activeQuery = useActiveQueryId();

  const fieldColumns = fields.toArray().map(({ field: fieldName, source }, i) => _c(
    fieldName,
    item[fieldName],
    fullValuePathForField(fieldName, valuePath).slice(0, i + 1),
    source,
  ));
  const columnPivotFields = columnPivotValues.flatMap((columnPivotValueKeys) => {
    const translatedPath = columnPivotValueKeys.flatMap((value, idx) => [columnPivots[idx], value]);
    const parentValuePath = [...valuePath];

    for (let i = 0; i < translatedPath.length; i += 2) {
      const [k, v]: Array<string> = translatedPath.slice(i, i + 2);
      parentValuePath.push({ [k]: v });
    }

    return series.map(({ effectiveName, function: fn }) => {
      const fullPath = [].concat(translatedPath, [effectiveName]);
      const value = get(item, fullPath);

      return _c(effectiveName, value, fullValuePathForField(fn, parentValuePath), fn);
    });
  });

  const columns = flatten([fieldColumns, columnPivotFields]);

  return (
    <tr className={`fields-row ${classes}`}>
      {columns.map(({ field, value, path, source }, idx) => {
        const key = `${activeQuery}-${field}=${value}-${idx}`;

        return (
          <Column key={key}
                  field={field}
                  value={value}
                  type={fieldTypeFor(columnNameToField(field, series), types)}
                  valuePath={path.slice()}
                  source={source} />
        );
      })}
    </tr>
  );
};

export default DataTableEntry;
