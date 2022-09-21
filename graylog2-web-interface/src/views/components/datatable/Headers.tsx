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
import React, { useEffect, useRef } from 'react';
import { flatten, get, isEqual, last, isNumber } from 'lodash';
import styled, { css } from 'styled-components';
import type { OrderedMap } from 'immutable';

import Field from 'views/components/Field';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Value from 'views/components/Value';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type Series from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import FieldSortIcon from 'views/components/datatable/FieldSortIcon';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { Widgets } from 'views/stores/WidgetStore';
import { Icon } from 'components/common';

import styles from './DataTable.css';

const getStickyStyles = (stickyLeftMargin) => `
  position: sticky!important;
  left: ${stickyLeftMargin}px;
  z-index: 1;
`;
const StyledTh = styled.th(({ isNumeric, stickyLeftMargin }: { isNumeric: boolean, stickyLeftMargin: number }) => css`
  ${isNumeric ? 'text-align: right' : ''}
  ${isNumber(stickyLeftMargin) ? getStickyStyles(stickyLeftMargin) : ''}
`);

const CenteredTh = styled.th`
  text-align: center;
`;

type HeaderFilterProps = {
  activeQuery: string;
  fields: (FieldTypeMappingsList | Array<FieldTypeMapping>);
  field: string;
  prefix?: (string | number);
  span?: number;
  title?: string;
  onSortChange: (sortConfig: Array<SortConfig>) => Promise<Widgets>;
  sortConfigMap: OrderedMap<string, SortConfig>;
  sortable: boolean;
  sortType?: 'pivot' | 'series' | undefined
  _onSetRowPivotColumnsWidth?: (props: { field: string, offsetWidth: number }) => void
  stickyLeftMargin?: number | undefined,
  showPinIcon?: boolean,
  togglePin: (field: string) => void
}
const PinIcon = styled.button(({ theme }) => {
  return css`
    border: 0;
    background: transparent;
    padding: 5px;
    cursor: pointer;
    position: relative;
    color: ${theme.colors.gray[70]};
    &.active {
      color: ${theme.colors.gray[20]};
    }
  `;
});

const _headerField = ({ activeQuery, fields, field, prefix = '', span = 1, title = field, onSortChange, sortConfigMap, sortable, sortType, _onSetRowPivotColumnsWidth, stickyLeftMargin, showPinIcon = false, togglePin }: HeaderFilterProps) => {
  const type = fieldTypeFor(field, fields);
  const thRef = useRef(null);

  useEffect(() => {
    if (_onSetRowPivotColumnsWidth && thRef?.current?.offsetWidth) {
      _onSetRowPivotColumnsWidth({ field, offsetWidth: thRef.current.offsetWidth });
    }
  }, [_onSetRowPivotColumnsWidth, field, thRef]);

  return (
    <StyledTh ref={thRef} isNumeric={type.isNumeric()} key={`${prefix}${field}`} colSpan={span} className={styles.leftAligned} stickyLeftMargin={stickyLeftMargin}>
      <Field name={field} queryId={activeQuery} type={type}>{title}</Field>
      {showPinIcon && <PinIcon onClick={() => togglePin(field)} className={isNumber(stickyLeftMargin) ? 'active' : ''}><Icon name="thumbtack" /></PinIcon>}
      {sortable && sortType && (
      <FieldSortIcon fieldName={field}
                     onSortChange={onSortChange}
                     setLoadingState={() => {
                     }}
                     sortConfigMap={sortConfigMap}
                     type={sortType} />
      )}
    </StyledTh>
  );
};

const _headerFieldForValue = (activeQuery: string, field, value, span = 1, prefix = '') => (
  <CenteredTh key={`${prefix}${field}-${value}`} colSpan={span} className={styles.leftAligned}>
    <Value field={field} value={value} queryId={activeQuery} type={FieldType.Unknown}>{value}</Value>
  </CenteredTh>
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
  onSortChange: (sortConfig: Array<SortConfig>) => Promise<Widgets>;
  sortConfigMap: OrderedMap<string, SortConfig>;
  onSetRowPivotColumnsWidth: (props: { field: string, offsetWidth: number }) => void,
  stickyLeftMargins?: { [key: string]: number } | undefined,
  togglePin: (field: string) => void
};

const Headers = ({ activeQuery, columnPivots, fields, rowPivots, series, rollup, actualColumnPivotFields, onSortChange, sortConfigMap, onSetRowPivotColumnsWidth, stickyLeftMargins, togglePin }: Props) => {
  const rowFieldNames = rowPivots.map((pivot) => pivot.field);
  const columnFieldNames = columnPivots.map((pivot) => pivot.field);
  const headerField = ({ field, prefix = '', span = 1, title = field, sortable = false, sortType = undefined, _onSetRowPivotColumnsWidth = undefined, showPinIcon = false }) => _headerField({
    activeQuery,
    fields,
    field,
    prefix,
    span,
    title,
    onSortChange,
    sortConfigMap,
    sortable,
    sortType,
    _onSetRowPivotColumnsWidth,
    stickyLeftMargin: stickyLeftMargins[field],
    showPinIcon,
    togglePin,
  });
  const rowPivotFields = rowFieldNames.map((fieldName) => headerField({ field: fieldName, sortable: true, sortType: SortConfig.PIVOT_TYPE, _onSetRowPivotColumnsWidth: onSetRowPivotColumnsWidth, showPinIcon: true }));
  const seriesFields = series.map((s) => headerField({ field: s.function, prefix: '', span: 1, title: s.effectiveName, sortable: true, sortType: SortConfig.SERIES_TYPE }));
  const columnPivotFields = flatten(actualColumnPivotFields.map((key) => series.map((s) => headerField({ field: s.function, prefix: key.join('-'), span: 1, title: s.effectiveName, sortable: false }))));
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

Headers.defaultProps = {
  stickyLeftMargins: undefined,
};

export default Headers;
