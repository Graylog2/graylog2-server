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
import { flatten, isEqual, uniqWith } from 'lodash';
import type { OrderedMap } from 'immutable';
import { FormikContext } from 'formik';
import styled, { css } from 'styled-components';

import connect from 'stores/connect';
import expandRows from 'views/logic/ExpandRows';
import { defaultCompare } from 'logic/DefaultCompare';
import { ViewStore } from 'views/stores/ViewStore';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { Leaf, Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { Events } from 'views/logic/searchtypes/events/EventHandler';
import { WidgetActions } from 'views/stores/WidgetStore';
import type SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import WidgetContext from 'views/components/contexts/WidgetContext';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';

import DataTableEntry from './DataTableEntry';
import MessagesTable from './MessagesTable';
import deduplicateValues from './DeduplicateValues';
import Headers from './Headers';
import styles from './DataTable.css';

import RenderCompletionCallback from '../widgets/RenderCompletionCallback';
import type { VisualizationComponentProps } from '../aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from '../aggregationbuilder/AggregationBuilder';

type Props = VisualizationComponentProps & {
  config: AggregationWidgetConfig,
  currentView: {
    activeQuery: string,
  },
  data: { [key: string]: Rows } & { events?: Events },
  fields: FieldTypeMappingsList,
  striped?: boolean,
  bordered?: boolean,
  borderedHeader?: boolean,
  stickyHeader?: boolean,
  condensed?: boolean,
};

const getStylesForPinnedColumns = (tag: 'th'|'td', stickyLeftMarginsByColumnIndex: Array<{index: number, column: string, leftMargin: number}>) => {
  return stickyLeftMarginsByColumnIndex.map(({ index, leftMargin }) => `
    ${tag}:nth-child(${index + 1}) {
        position: sticky!important;
        left: ${leftMargin}px;
        z-index: 1;
    }
  `).concat((' ; '));
};

const THead = styled.thead(({ stickyLeftMarginsByColumnIndex }: {
  stickyLeftMarginsByColumnIndex: Array<{index: number, column: string, leftMargin: number}>
}) => css`
  & tr.pivot-header-row {
    & ${getStylesForPinnedColumns('th', stickyLeftMarginsByColumnIndex)}
  }
`);
const TBody = styled.tbody(({ stickyLeftMarginsByColumnIndex }: {
  stickyLeftMarginsByColumnIndex: Array<{index: number, column: string, leftMargin: number}>
}) => css`
  & tr {
    & ${getStylesForPinnedColumns('td', stickyLeftMarginsByColumnIndex)}
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
  currentView,
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
  const onSetColumnsWidth = useCallback(({ field, offsetWidth }: { field: string, offsetWidth: number}) => {
    setRowPivotColumnsWidth((cur) => {
      const copy = { ...cur };
      copy[field] = offsetWidth;

      return copy;
    });
  }, [setRowPivotColumnsWidth]);
  const _onSortChange = useCallback((newSort: Array<SortConfig>) => {
    const dirty = formContext?.dirty;
    const updateWidget = () => WidgetActions.updateConfig(widget.id, config.toBuilder().sort(newSort).build());
    if (!editing || (editing && !dirty)) return updateWidget();

    // eslint-disable-next-line no-alert
    if (window.confirm('You have unsaved changes in configuration form. This action will rollback them')) {
      return updateWidget();
    }

    return Promise.reject();
  }, [config, widget, editing, formContext]);

  const togglePin = useCallback((field: string) => {
    const dirty = formContext?.dirty;

    const updateWidget = () => {
      const curVisualizationConfig = widget.config.visualizationConfig || DataTableVisualizationConfig.create([]).toBuilder().build();
      let pinned_columns;

      if (curVisualizationConfig?.pinned_columns?.has(field)) {
        pinned_columns = curVisualizationConfig.pinned_columns.delete(field);
      } else {
        pinned_columns = curVisualizationConfig.pinned_columns.add(field);
      }

      return WidgetActions.updateConfig(widget.id, widget.config.toBuilder().visualizationConfig(curVisualizationConfig.toBuilder().pinned_columns(pinned_columns.toJS()).build()).build());
    };

    if (!editing || (editing && !dirty)) return updateWidget();

    // eslint-disable-next-line no-alert
    if (window.confirm('You have unsaved changes in configuration form. This action will rollback them')) {
      return updateWidget();
    }

    return Promise.reject();
  }, [widget, editing, formContext]);

  const { columnPivots, rowPivots, series, rollup } = config;
  const rows = retrieveChartData(data) ?? [];

  const rowFieldNames = rowPivots.map<string>((pivot) => pivot.field);
  const columnFieldNames = columnPivots.map((pivot) => pivot.field);

  const seriesToMerge = rollup ? series : [];
  const effectiveFields = Immutable.OrderedSet(rowFieldNames.map((field) => ({ field, source: field })))
    .merge(seriesToMerge.map((s) => ({ field: s.effectiveName, source: s.function })));

  const expandedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), rows.filter((r): r is Leaf => r.source === 'leaf'));

  const actualColumnPivotFields = _extractColumnPivotValues(rows);
  const pinnedColumns = useMemo(() => {
    return widget?.config?.visualizationConfig?.pinned_columns || Immutable.Set();
  }, [widget?.config?.visualizationConfig?.pinned_columns]);

  const stickyLeftMarginsByColumnIndex = useMemo(() => {
    let prev = 0;
    const res = [];

    rowPivots.forEach((row, index) => {
      if (pinnedColumns.has(row.field)) {
        const column = row.field;
        res.push({ index, column, leftMargin: prev });
        prev += rowPivotColumnsWidth[row.field];
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

    return (
      // eslint-disable-next-line react/no-array-index-key
      <DataTableEntry key={`datatableentry-${idx}`}
                      fields={effectiveFields}
                      item={reducedItem}
                      valuePath={valuePath}
                      currentView={currentView}
                      columnPivots={columnFieldNames}
                      columnPivotValues={actualColumnPivotFields}
                      types={fields}
                      series={series} />
    );
  });

  const sortConfigMap = useMemo<OrderedMap<string, SortConfig>>(() => Immutable.OrderedMap(config.sort.map((sort) => [sort.field, sort])), [config]);

  return (
    <div className={styles.container}>
      <div className={styles.scrollContainer}>
        <MessagesTable striped={striped}
                       bordered={bordered}
                       borderedHeader={borderedHeader}
                       stickyHeader={stickyHeader}
                       condensed={condensed}>
          <THead stickyLeftMarginsByColumnIndex={stickyLeftMarginsByColumnIndex}>
            <Headers activeQuery={currentView.activeQuery}
                     actualColumnPivotFields={actualColumnPivotFields}
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
          <TBody stickyLeftMarginsByColumnIndex={stickyLeftMarginsByColumnIndex}>
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

const ConnectedDataTable = makeVisualization(connect(DataTable, { currentView: ViewStore }), 'table');

export default ConnectedDataTable;
