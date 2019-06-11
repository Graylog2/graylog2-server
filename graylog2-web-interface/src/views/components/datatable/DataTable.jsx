// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { flatten, isEqual, uniqWith } from 'lodash';

import expandRows from 'enterprise/logic/ExpandRows';
import { defaultCompare } from 'enterprise/logic/DefaultCompare';
// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

import { ViewStore } from 'enterprise/stores/ViewStore';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'enterprise/stores/FieldTypesStore';
import type { Rows } from 'enterprise/logic/searchtypes/pivot/PivotHandler';
import DataTableEntry from './DataTableEntry';

import deduplicateValues from './DeduplicateValues';
import Headers from './Headers';
import styles from './DataTable.css';

type Props = {
  config: AggregationWidgetConfig,
  currentView: {
    activeQuery: string,
  },
  data: Rows,
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
  const { columnPivots, rowPivots, series, rollup } = config;
  const rows = data || [];

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
    <div className={`messages-container ${styles.container}`}>
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
  );
};

const ConnectedDataTable = connect(DataTable, { currentView: ViewStore });
ConnectedDataTable.type = 'table';

export default ConnectedDataTable;
