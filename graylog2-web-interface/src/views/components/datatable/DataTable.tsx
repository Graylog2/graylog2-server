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
import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as Immutable from 'immutable';
import flatten from 'lodash/flatten';
import isEqual from 'lodash/isEqual';
import uniqWith from 'lodash/uniqWith';
import type { OrderedMap } from 'immutable';
import { FormikContext } from 'formik';
import styled, { css } from 'styled-components';

import expandRows from 'views/logic/ExpandRows';
import { defaultCompare } from 'logic/DefaultCompare';
import type { Leaf, Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { Events } from 'views/logic/searchtypes/events/EventHandler';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import WidgetContext from 'views/components/contexts/WidgetContext';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import useAppDispatch from 'stores/useAppDispatch';
import { updateWidgetConfig } from 'views/logic/slices/widgetActions';

import DataTableEntry from './DataTableEntry';
import MessagesTable from './MessagesTable';
import deduplicateValues from './DeduplicateValues';
import Headers from './Headers';
import styles from './DataTable.css';

import RenderCompletionCallback from '../widgets/RenderCompletionCallback';
import type { VisualizationComponentProps } from '../aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from '../aggregationbuilder/AggregationBuilder';

type Props = VisualizationComponentProps & {
  data: { [key: string]: Rows } & { events?: Events },
  striped?: boolean,
  bordered?: boolean,
  borderedHeader?: boolean,
  stickyHeader?: boolean,
  condensed?: boolean,
};

const getStylesForPinnedColumns = (tag: 'th'|'td', stickyLeftMarginsByColumnIndex: Array<{index: number, column: string, leftMargin: number}>) => stickyLeftMarginsByColumnIndex.map(({ index, leftMargin }) => `
    ${tag}:nth-child(${index + 1}) {
        position: sticky!important;
        left: ${leftMargin}px;
        z-index: 1;
    }
  `).concat((' ; '));

const THead = styled.thead<{
    $stickyLeftMarginsByColumnIndex: Array<{index: number, column: string, leftMargin: number}>
}>(({ $stickyLeftMarginsByColumnIndex }) => css`
  & tr.pivot-header-row {
    & ${getStylesForPinnedColumns('th', $stickyLeftMarginsByColumnIndex)}
  }
`);

const TBody = styled.tbody<{
  $stickyLeftMarginsByColumnIndex: Array<{index: number, column: string, leftMargin: number}>
}>(({ $stickyLeftMarginsByColumnIndex }) => css`
  & tr {
    & ${getStylesForPinnedColumns('td', $stickyLeftMarginsByColumnIndex)}
  }
`);

const _compareArray = (ary1, ary2) => {
  if (ary1 === undefined) {
    if (ary2 === undefined) {
      return 0;
    }

    return -1;
  }

  if (ary1.length > ary2.length) {
    return 1;
  }

  if (ary1.length < ary2.length) {
    return -1;
  }

  const diffIdx = ary1.findIndex((v, idx) => (defaultCompare(v, ary2[idx]) !== 0));

  if (diffIdx === -1) {
    return 0;
  }

  return defaultCompare(ary1[diffIdx], ary2[diffIdx]);
};

const _extractColumnPivotValues = (rows): Array<Array<string>> => {
  const uniqRows = uniqWith(
    flatten(
      rows
        .filter(({ source }) => (source === 'leaf' || source === 'non-leaf'))
        .map(({ values }) => values),
    )
      .filter(({ rollup }) => !rollup)
      .map(({ key }) => key.slice(0, -1)),
    isEqual,
  );

  return Immutable.List<Array<string>>(uniqRows).sort(_compareArray).toArray();
};

const DataTable = ({
  config,
  data,
  fields,
  striped,
  bordered,
  borderedHeader,
  stickyHeader,
  condensed,
  editing,
}: Props) => {
  const formContext = useContext(FormikContext);
  const onRenderComplete = useContext(RenderCompletionCallback);
  const widget = useContext(WidgetContext);
  useEffect(onRenderComplete, [onRenderComplete]);
  const [rowPivotColumnsWidth, setRowPivotColumnsWidth] = useState<{ [key: string]: number }>({});
  const dispatch = useAppDispatch();

  const onSetColumnsWidth = useCallback(({ field, offsetWidth }: { field: string, offsetWidth: number}) => {
    setRowPivotColumnsWidth((cur) => {
      const copy = { ...cur };
      copy[field] = offsetWidth;

      return copy;
    });
  }, [setRowPivotColumnsWidth]);
  const _onSortChange = useCallback((newSort: Array<SortConfig>) => {
    const dirty = formContext?.dirty;
    const updateWidget = () => dispatch(updateWidgetConfig(widget.id, config.toBuilder().sort(newSort).build()));

    if (!editing || (editing && !dirty)) {
      return updateWidget();
    }

    // eslint-disable-next-line no-alert
    if (window.confirm('You have unsaved changes in configuration form. This action will rollback them')) {
      return updateWidget();
    }

    return Promise.reject();
  }, [formContext?.dirty, editing, dispatch, widget?.id, config]);

  const togglePin = useCallback((field: string) => {
    const dirty = formContext?.dirty;

    const updateWidget = () => {
      const curVisualizationConfig = widget.config.visualizationConfig ?? DataTableVisualizationConfig.create([]).toBuilder().build();
      const pinnedColumns = curVisualizationConfig?.pinnedColumns?.has(field)
        ? curVisualizationConfig.pinnedColumns.delete(field)
        : curVisualizationConfig.pinnedColumns.add(field);

      return dispatch(updateWidgetConfig(
        widget.id,
        widget
          .config
          .toBuilder()
          .visualizationConfig(
            curVisualizationConfig
              .toBuilder()
              .pinnedColumns(pinnedColumns.toJS())
              .build())
          .build()));
    };

    if (!editing || (editing && !dirty)) {
      return updateWidget();
    }

    // eslint-disable-next-line no-alert
    if (window.confirm('You have unsaved changes in configuration form. This action will rollback them')) {
      return updateWidget();
    }

    return Promise.reject();
  }, [formContext?.dirty, editing, widget?.config, widget?.id, dispatch]);

  const { columnPivots, rowPivots, series, rollupForBackendQuery: rollup } = config;

  const rows = retrieveChartData(data) ?? [];

  const rowFieldNames = rowPivots.flatMap((pivot) => pivot.fields);
  const columnFieldNames = columnPivots.flatMap((pivot) => pivot.fields);

  const seriesToMerge = rollup ? series : [];
  const effectiveFields = Immutable.OrderedSet(rowFieldNames.map((field) => ({ field, source: field })))
    .merge(seriesToMerge.map((s) => ({ field: s.effectiveName, source: s.function })));

  const expandedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), rows.filter((r): r is Leaf => r.source === 'leaf'));

  const actualColumnPivotFields = _extractColumnPivotValues(rows);
  const pinnedColumns = useMemo(() => widget?.config?.visualizationConfig?.pinnedColumns || Immutable.Set(), [widget?.config?.visualizationConfig?.pinnedColumns]);

  const stickyLeftMarginsByColumnIndex = useMemo(() => {
    let prev = 0;
    const res = [];

    const rowPivotsFields = rowPivots.flatMap((rowPivot) => rowPivot.fields ?? []);

    rowPivotsFields.forEach((field, index) => {
      if (pinnedColumns.has(field)) {
        const column = field;
        res.push({ index, column, leftMargin: prev });
        prev += rowPivotColumnsWidth[field];
      }
    });

    series.forEach((row, index) => {
      if (pinnedColumns.has(row.function)) {
        const column = row.function;
        res.push({ index: index + rowPivots.length, column, leftMargin: prev });
        prev += rowPivotColumnsWidth[row.function];
      }
    });

    return res;
  }, [rowPivotColumnsWidth, rowPivots, pinnedColumns, series]);
  const formattedRows = deduplicateValues(expandedRows, rowFieldNames).map((reducedItem, idx) => {
    const valuePath = rowFieldNames.map((pivotField) => ({ [pivotField]: expandedRows[idx][pivotField] }));
    const key = `datatableentry-${idx}`;

    return (

      (
        <DataTableEntry key={key}
                        fields={effectiveFields}
                        item={reducedItem}
                        valuePath={valuePath}
                        columnPivots={columnFieldNames}
                        columnPivotValues={actualColumnPivotFields}
                        types={fields}
                        series={series} />
      )
    );
  });

  const sortConfigMap = useMemo<OrderedMap<string, SortConfig>>(() => Immutable.OrderedMap(config.sort.map((sort) => [sort.field, sort])), [config]);

  return (
    <div className={styles.container}>
      <div className={styles.scrollContainer}>
        <MessagesTable striped={striped}
                       bordered={bordered}
                       stickyHeader={stickyHeader}
                       condensed={condensed}>
          <THead $stickyLeftMarginsByColumnIndex={stickyLeftMarginsByColumnIndex}>
            <Headers actualColumnPivotFields={actualColumnPivotFields}
                     borderedHeader={borderedHeader}
                     columnPivots={columnPivots}
                     fields={fields}
                     rollup={rollup}
                     rowPivots={rowPivots}
                     series={series}
                     onSortChange={_onSortChange}
                     sortConfigMap={sortConfigMap}
                     onSetColumnsWidth={onSetColumnsWidth}
                     pinnedColumns={pinnedColumns}
                     togglePin={togglePin} />
          </THead>
          <TBody $stickyLeftMarginsByColumnIndex={stickyLeftMarginsByColumnIndex}>
            {formattedRows}
          </TBody>
        </MessagesTable>
      </div>
    </div>
  );
};

DataTable.defaultProps = {
  condensed: true,
  striped: true,
  bordered: false,
  stickyHeader: true,
  borderedHeader: true,
};

const ConnectedDataTable = makeVisualization(DataTable, 'table');

export default ConnectedDataTable;
