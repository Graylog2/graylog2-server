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
// @flow strict
import * as React from 'react';
import { useContext, useEffect } from 'react';
import * as Immutable from 'immutable';
import { flatten, isEqual, uniqWith } from 'lodash';

import connect from 'stores/connect';
import expandRows from 'views/logic/ExpandRows';
import { defaultCompare } from 'views/logic/DefaultCompare';
import { ViewStore } from 'views/stores/ViewStore';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

import DataTableEntry from './DataTableEntry';
import MessagesTable from './MessagesTable';
import deduplicateValues from './DeduplicateValues';
import Headers from './Headers';
import styles from './DataTable.css';

import RenderCompletionCallback from '../widgets/RenderCompletionCallback';
import type { VisualizationComponent } from '../aggregationbuilder/AggregationBuilder';
import { makeVisualization } from '../aggregationbuilder/AggregationBuilder';

type Props = {
  config: AggregationWidgetConfig,
  currentView: {
    activeQuery: string,
  },
  data: { chart: Rows },
  fields: FieldTypeMappingsList,
};

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
        // $FlowFixMe: Actually filtering out rows with single values
        .map(({ values }) => values),
    )
      // $FlowFixMe: Should be safe, even if rollup is not present
      .filter(({ rollup }) => !rollup)
      .map(({ key }) => key.slice(0, -1)),
    isEqual,
  );

  return Immutable.List(uniqRows).sort(_compareArray).toArray();
};

const DataTable = ({ config, currentView, data, fields }: Props) => {
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(onRenderComplete, [onRenderComplete]);

  const { columnPivots, rowPivots, series, rollup } = config;
  const rows = data.chart || Object.values(data)[0] || [];

  const rowFieldNames = rowPivots.map<string>((pivot) => pivot.field);
  const columnFieldNames = columnPivots.map((pivot) => pivot.field);

  const seriesToMerge = rollup ? series : [];
  const effectiveFields = Immutable.OrderedSet(rowFieldNames.map((field) => ({ field, source: field })))
    .merge(seriesToMerge.map((s) => ({ field: s.effectiveName, source: s.function })));

  const expandedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), rows.filter((r) => r.source === 'leaf'));

  const actualColumnPivotFields = _extractColumnPivotValues(rows);

  const formattedRows = deduplicateValues(expandedRows, rowFieldNames).map((reducedItem, idx) => {
    const valuePath = rowFieldNames.map((pivotField) => ({ [pivotField]: expandedRows[idx][pivotField] }));

    // eslint-disable-next-line react/no-array-index-key
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

  return (
    <div className={styles.container}>
      <div className={styles.scrollContainer}>
        <MessagesTable>
          <thead>
            <Headers activeQuery={currentView.activeQuery}
                     actualColumnPivotFields={actualColumnPivotFields}
                     columnPivots={columnPivots}
                     fields={fields}
                     rollup={rollup}
                     rowPivots={rowPivots}
                     series={series} />
          </thead>
          {formattedRows}
        </MessagesTable>
      </div>
    </div>
  );
};

const ConnectedDataTable: VisualizationComponent = makeVisualization(connect(DataTable, { currentView: ViewStore }), 'table');

export default ConnectedDataTable;
