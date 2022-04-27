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
import { flatten, get } from 'lodash';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import Value from 'views/components/Value';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import type { ValuePath } from 'views/logic/valueactions/ValueActionHandler';
import type Series from 'views/logic/aggregationbuilder/Series';
import { parseSeries } from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';

import type { CurrentViewType } from '../CustomPropTypes';
import CustomHighlighting from '../messagelist/CustomHighlighting';
import DecoratedValue from '../messagelist/decoration/DecoratedValue';

const StyledTd = styled.td(({ isNumeric, theme }: { isNumeric: boolean, theme: DefaultTheme }) => css`
  ${isNumeric ? `font-family: ${theme.fonts.family.monospace};` : ''}
  ${isNumeric ? 'text-align: right' : ''}
`);

type Field = {
  field: string,
  source: string,
};
type Props = {
  columnPivots: Array<string>,
  columnPivotValues: Array<Array<string>>,
  currentView: CurrentViewType,
  fields: Immutable.Set<Field>,
  item: { [key: string]: any },
  series: Array<Series>,
  types: FieldTypeMappingsList,
  valuePath: ValuePath,
};

const _c = (field, value, path, source) => ({ field, value, path, source });

const _column = (field: string, value: any, selectedQuery: string, idx: number, type: FieldType, valuePath: ValuePath, source: string | undefined | null) => {
  const additionalContextValue = useMemo(() => ({ valuePath }), [valuePath]);

  return (
    <StyledTd isNumeric={type.isNumeric()} key={`${selectedQuery}-${field}=${value}-${idx}`}>
      <AdditionalContext.Provider value={additionalContextValue}>
        <CustomHighlighting field={source ?? field} value={value}>
          {value !== null && value !== undefined
            ? (
              <Value field={source ?? field}
                     type={type}
                     value={value}
                     queryId={selectedQuery}
                     render={DecoratedValue} />
            ) : null}
        </CustomHighlighting>
      </AdditionalContext.Provider>
    </StyledTd>
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

const DataTableEntry = ({ columnPivots, currentView, fields, series, columnPivotValues, valuePath, item, types }: Props) => {
  const classes = 'message-group';
  const { activeQuery } = currentView;

  const fieldColumns = fields.toSeq().toJS().map(({ field: fieldName, source }, i) => _c(
    fieldName,
    item[fieldName],
    fullValuePathForField(fieldName, valuePath).slice(0, i + 1),
    source,
  ));
  const columnPivotFields = flatten(columnPivotValues.map((columnPivotValueKeys) => {
    const translatedPath = flatten(columnPivotValueKeys.map((value, idx) => [columnPivots[idx], value]));
    const [k, v]: Array<string> = translatedPath;
    const parentValuePath = [...valuePath, { [k]: v }];

    return series.map(({ effectiveName, function: fn }) => {
      const fullPath = [].concat(translatedPath, [effectiveName]);
      const value = get(item, fullPath);

      return _c(effectiveName, value, fullValuePathForField(fn, parentValuePath), fn);
    });
  }));

  const columns = flatten([fieldColumns, columnPivotFields]);

  return (
    <tbody className={classes}>
      <tr className="fields-row">
        {columns.map(({ field, value, path, source }, idx) => _column(field, value, activeQuery, idx, fieldTypeFor(columnNameToField(field, series), types), path.slice(), source))}
      </tr>
    </tbody>
  );
};

export default DataTableEntry;
