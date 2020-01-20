// @flow strict
import React, { useContext, useEffect } from 'react';
import * as Immutable from 'immutable';
import { flatten, isEqual, uniqWith } from 'lodash';

import expandRows from 'views/logic/ExpandRows';
import { defaultCompare } from 'views/logic/DefaultCompare';
import connect from 'stores/connect';

import { ViewStore } from 'views/stores/ViewStore';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import DataTableEntry from './DataTableEntry';

import deduplicateValues from './DeduplicateValues';
import Headers from './Headers';
import styles from './DataTable.css';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

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
  const { chart: rows = [] } = data || {};

  const rowFieldNames = rowPivots.map(pivot => pivot.field);
  const columnFieldNames = columnPivots.map(pivot => pivot.field);

  const seriesToMerge = rollup ? series : [];
  const effectiveFields = new Immutable.OrderedSet(rowFieldNames).merge(seriesToMerge.map(({ effectiveName }) => effectiveName));

  const expandedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), rows.filter(r => r.source === 'leaf'));

  const actualColumnPivotFields = _extractColumnPivotValues(rows);

  const formattedRows = deduplicateValues(expandedRows, rowFieldNames).map((reducedItem, idx) => {
    const valuePath = rowFieldNames.map(pivotField => ({ [pivotField]: expandedRows[idx][pivotField] }));
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
        <div className="messages-container">
          <table className="table table-condensed messages">
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
          </table>
        </div>
      </div>
    </div>
  );
};

const ConnectedDataTable = connect(DataTable, { currentView: ViewStore });
ConnectedDataTable.type = 'table';

export default ConnectedDataTable;
