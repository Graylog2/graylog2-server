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
import { useCallback, useContext, useLayoutEffect, useRef } from 'react';
import flatten from 'lodash/flatten';
import get from 'lodash/get';
import isEqual from 'lodash/isEqual';
import last from 'lodash/last';
import styled, { css } from 'styled-components';
import type { OrderedMap } from 'immutable';
import Immutable from 'immutable';

import Field from 'views/components/Field';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import Value from 'views/components/Value';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type Series from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { Icon } from 'components/common';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import InteractiveContext from 'views/components/contexts/InteractiveContext';

import FieldSortIcon from './FieldSortIcon';
import TableHeaderCell from './TableHeaderCell';

const CenteredTh = styled(TableHeaderCell)`
  text-align: center;
`;

const PinIcon = styled.button(({ theme }) => css`
  border: 0;
  background: transparent;
  padding: 5px;
  cursor: pointer;
  position: relative;
  color: ${theme.colors.gray[70]};

  &.active {
    color: ${theme.colors.gray[20]};
  }
`);

type HeaderFilterProps = {
  activeQuery: string;
  borderedHeader: boolean,
  fields: (FieldTypeMappingsList | Array<FieldTypeMapping>);
  field: string;
  prefix?: (string | number);
  span?: number;
  title?: string;
  onSortChange: (sortConfig: Array<SortConfig>) => Promise<unknown>;
  sortConfigMap: OrderedMap<string, SortConfig>;
  sortable: boolean;
  sortType?: 'pivot' | 'series' | undefined
  onSetColumnsWidth?: (props: { field: string, offsetWidth: number }) => void
  isPinned?: boolean | undefined,
  showPinIcon?: boolean,
  togglePin: (field: string) => void,
}

const HeaderField = ({
  activeQuery,
  borderedHeader,
  fields,
  field,
  prefix = '',
  span = 1,
  title = field,
  onSortChange,
  sortConfigMap,
  sortable,
  sortType,
  onSetColumnsWidth,
  isPinned,
  showPinIcon = false,
  togglePin,
}: HeaderFilterProps) => {
  const type = fieldTypeFor(field, fields);
  const thRef = useRef(null);

  useLayoutEffect(() => {
    if (onSetColumnsWidth && thRef?.current?.offsetWidth) {
      onSetColumnsWidth({ field: `${prefix}${field}`, offsetWidth: thRef.current.offsetWidth });
    }
  }, [onSetColumnsWidth, field, prefix, thRef?.current?.offsetWidth]);

  const _togglePin = useCallback(() => {
    togglePin(`${prefix}${field}`);
  }, [togglePin, prefix, field]);

  return (
    <TableHeaderCell ref={thRef}
                     key={`${prefix}${field}`}
                     colSpan={span}
                     $isNumeric={type.isNumeric()}
                     $borderedHeader={borderedHeader}>
      <Field name={field} queryId={activeQuery} type={type}>{title}</Field>
      {showPinIcon && <PinIcon data-testid={`pin-${prefix}${field}`} type="button" onClick={_togglePin} className={isPinned ? 'active' : ''}><Icon name="thumbtack" /></PinIcon>}
      {sortable && sortType && (
      <FieldSortIcon fieldName={field}
                     onSortChange={onSortChange}
                     setLoadingState={() => {}}
                     sortConfigMap={sortConfigMap}
                     type={sortType} />
      )}
    </TableHeaderCell>
  );
};

HeaderField.defaultProps = {
  prefix: undefined,
  span: undefined,
  title: undefined,
  sortType: undefined,
  onSetColumnsWidth: undefined,
  isPinned: undefined,
  showPinIcon: undefined,
};

type HeaderFieldForValueProps = {
  borderedHeader: boolean,
  field: string,
  value: any,
  span?: number,
  prefix?: string,
  type: FieldType,
};
const HeaderFieldForValue = ({ field, value, span = 1, prefix = '', type, borderedHeader }: HeaderFieldForValueProps) => (
  <CenteredTh key={`${prefix}${field}-${value}`}
              colSpan={span}
              $borderedHeader={borderedHeader}>
    <Value field={field} value={value} type={type} />
  </CenteredTh>
);

HeaderFieldForValue.defaultProps = {
  span: 1,
  prefix: '',
};

const Spacer = ({ span }: { span: number }) => <th aria-label="spacer" colSpan={span} />;

type ColumnHeadersProps = {
  borderedHeader: boolean,
  fields: (FieldTypeMappingsList | Array<FieldTypeMapping>);
  pivots: string[],
  values: any[][],
  series: Series[],
  offset?: number,
};

const ColumnPivotFieldsHeaders = ({ fields, pivots, values, series, offset = 1, borderedHeader }: ColumnHeadersProps) => {
  const headerRows = pivots.map((columnPivot, idx) => {
    const actualValues = values.map((key) => ({ path: key.slice(0, idx).join('-'), key: key[idx] || '', count: 1 }));
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

    const type = fieldTypeFor(columnPivot, fields);

    return (
      <tr key={`header-table-row-${columnPivot}`}>
        {offset > 0 && <Spacer span={offset} />}
        {actualValuesWithoutDuplicates.map((value) => (
          <HeaderFieldForValue key={`header-field-value-${value.path}-${value.key}`}
                               borderedHeader={borderedHeader}
                               field={columnPivot}
                               value={value.key}
                               span={value.count * series.length}
                               prefix={value.path}
                               type={type} />
        ))}
      </tr>
    );
  });

  // eslint-disable-next-line react/jsx-no-useless-fragment
  return <>{headerRows}</>;
};

ColumnPivotFieldsHeaders.defaultProps = {
  offset: 1,
};

type Props = {
  borderedHeader: boolean,
  columnPivots: Array<Pivot>,
  rowPivots: Array<Pivot>,
  series: Array<Series>,
  rollup: boolean,
  actualColumnPivotFields: Array<Array<string>>,
  fields: FieldTypeMappingsList,
  onSortChange: (sortConfig: Array<SortConfig>) => Promise<unknown>;
  sortConfigMap: OrderedMap<string, SortConfig>;
  onSetColumnsWidth: (props: { field: string, offsetWidth: number }) => void,
  pinnedColumns?: Immutable.Set<string>
  togglePin: (field: string) => void
};

const Headers = ({
  borderedHeader,
  columnPivots,
  fields,
  rowPivots,
  series,
  rollup,
  actualColumnPivotFields,
  onSortChange,
  sortConfigMap,
  onSetColumnsWidth,
  pinnedColumns,
  togglePin,
}: Props) => {
  const activeQuery = useActiveQueryId();
  const rowFieldNames = rowPivots.flatMap((pivot) => pivot.fields);
  const columnFieldNames = columnPivots.flatMap((pivot) => pivot.fields);
  const interactive = useContext(InteractiveContext);

  const headerField = ({ field, prefix = '', span = 1, title = field, sortable = false, sortType = undefined, showPinIcon = false }) => (
    <HeaderField activeQuery={activeQuery}
                 borderedHeader={borderedHeader}
                 key={`${prefix}${field}`}
                 fields={fields}
                 field={field}
                 prefix={prefix}
                 span={span}
                 title={title}
                 onSortChange={onSortChange}
                 sortConfigMap={sortConfigMap}
                 sortable={sortable}
                 sortType={sortType}
                 onSetColumnsWidth={onSetColumnsWidth}
                 isPinned={pinnedColumns.has(`${prefix}${field}`)}
                 showPinIcon={showPinIcon}
                 togglePin={togglePin} />
  );

  const rowPivotFields = rowFieldNames.map((fieldName) => headerField({ field: fieldName, sortable: interactive, sortType: SortConfig.PIVOT_TYPE, showPinIcon: interactive }));
  const seriesFields = series.map((s) => headerField({ field: s.function, prefix: '', span: 1, title: s.effectiveName, sortable: interactive, sortType: SortConfig.SERIES_TYPE, showPinIcon: false }));
  const columnPivotFields = flatten(actualColumnPivotFields.map((key) => series.map((s) => headerField({ field: s.function, prefix: key.join('-'), span: 1, title: s.effectiveName, sortable: false, showPinIcon: false }))));
  const offset = rollup ? rowFieldNames.length + series.length : rowFieldNames.length;

  return (
    <>
      <ColumnPivotFieldsHeaders borderedHeader={borderedHeader}
                                fields={fields}
                                pivots={columnFieldNames}
                                values={actualColumnPivotFields}
                                series={series}
                                offset={offset} />
      <tr className="pivot-header-row">
        {rowPivotFields}
        {rollup && seriesFields}
        {columnPivotFields}
      </tr>
    </>
  );
};

Headers.defaultProps = {
  pinnedColumns: Immutable.Set(),
};

export default Headers;
