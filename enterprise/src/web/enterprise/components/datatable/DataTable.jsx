import React from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import Immutable from 'immutable';
import { flatten, get, isEqual, last, uniqWith } from 'lodash';

import expandRows from 'enterprise/logic/ExpandRows';
import { defaultCompare } from 'enterprise/logic/DefaultCompare';
import connect from 'stores/connect';

import Field from 'enterprise/components/Field';
import Value from 'enterprise/components/Value';
import { ViewStore } from 'enterprise/stores/ViewStore';
import FieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import DataTableEntry from './DataTableEntry';
import deduplicateValues from './DeduplicateValues';

class DataTable extends React.Component {
  static propTypes = {
    config: AggregationType.isRequired,
    currentView: PropTypes.shape({
      activeQuery: PropTypes.string.isRequired,
    }).isRequired,
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    fields: ImmutablePropTypes.listOf(PropTypes.instanceOf(FieldTypeMapping)).isRequired,
  };

  _fieldTypeFor = (field) => {
    const fieldType = this.props.fields.find(f => f.name === field);
    return fieldType ? fieldType.type : FieldType.Unknown;
  };

  _headerField = (field, prefix = '', span = 1) => (
    <th key={`${prefix}${field}`} colSpan={span} style={{ left: '0px' }}>
      <Field name={field} queryId={this.props.currentView.activeQuery} type={this._fieldTypeFor(field)}>{field}</Field>
    </th>
  );

  _headerFieldForValue = (field, value, span = 1, prefix = '') => (
    <th key={`${prefix}${field}-${value}`} colSpan={span} style={{ left: '0px' }}>
      <Value field={field} value={value} queryId={this.props.currentView.activeQuery}>{value}</Value>
    </th>
  );

  _spacer = (idx, span = 1) => <th colSpan={span} key={`spacer-${idx}`} style={{ left: '0px' }} />;

  _columnPivotHeaders = (columnPivots, actualColumnPivotValues, series, offset = 1) => {
    return columnPivots.map((columnPivot, idx) => {
      const actualValues = actualColumnPivotValues.map(key => ({ path: key.slice(0, idx).join('-'), key: key[idx] || '', count: 1 }));
      const actualValuesWithoutDuplicates = actualValues.reduce((prev, cur) => {
        const lastKey = get(last(prev), 'key');
        const lastPath = get(last(prev), 'path');
        if (lastKey === cur.key && isEqual(lastPath, cur.path)) {
          const lastItem = last(prev);
          const remainder = prev.slice(0, -1);
          const newLastItem = Object.assign({}, lastItem, { count: lastItem.count + 1 });
          return [].concat(remainder, [newLastItem]);
        }
        return [].concat(prev, [cur]);
      }, []);
      return (
        <tr key={`header-table-row-${columnPivot}`}>
          {this._spacer(1, offset)}
          {actualValuesWithoutDuplicates.map(value => this._headerFieldForValue(columnPivot, value.key, value.count * series.length, value.path))}
        </tr>
      );
    });
  };

  _compareArray = (ary1, ary2) => {
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

  _extractColumnPivotValues = (rows) => {
    const uniqRows = uniqWith(flatten(rows.map(({ values }) => values))
      .filter(({ rollup }) => !rollup)
      .map(({ key }) => key.slice(0, -1)), isEqual);
    return Immutable.List(uniqRows).sort(this._compareArray).toJS();
  };

  render() {
    const { config, data } = this.props;
    const { columnPivots, rowPivots, series, rollup } = config;
    const rows = data || [];

    const rowFieldNames = rowPivots.map(pivot => pivot.field);
    const columnFieldNames = columnPivots.map(pivot => pivot.field);

    const seriesToMerge = rollup ? series : [];
    const fields = new Immutable.OrderedSet(rowFieldNames).merge(seriesToMerge.map(({ effectiveName }) => effectiveName));

    const expandedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), rows.filter(r => r.source === 'leaf'));

    const actualColumnPivotFields = this._extractColumnPivotValues(rows, columnFieldNames);
    const rowPivotFields = rowFieldNames.map(this._headerField);

    const offset = rollup ? rowFieldNames.length + series.length : 1;
    const columnPivotFieldsHeaders = this._columnPivotHeaders(columnFieldNames, actualColumnPivotFields, series, offset);
    const formattedRows = deduplicateValues(expandedRows, rowFieldNames).map((reducedItem, idx) => {
      // eslint-disable-next-line react/no-array-index-key
      return (<DataTableEntry key={`datatableentry-${idx}`}
                              fields={fields}
                              item={reducedItem}
                              currentView={this.props.currentView}
                              columnPivots={columnFieldNames}
                              columnPivotValues={actualColumnPivotFields}
                              types={this.props.fields}
                              series={series} />);
    });

    const effectiveSeries = series.map(s => s.effectiveName);
    const seriesFields = effectiveSeries.map(this._headerField);
    const columnPivotFields = flatten(actualColumnPivotFields.map(key => effectiveSeries.map(s => this._headerField(s, key.join('-')))));
    return (
      <div className="messages-container" style={{ overflow: 'auto', height: '100%' }}>
        <table className="table table-condensed messages">
          <thead>
            {columnPivotFieldsHeaders}
            <tr>
              {rowPivotFields}
              {rollup && seriesFields}
              {columnPivotFields}
            </tr>
          </thead>
          {formattedRows}
        </table>
      </div>
    );
  }
}

const ConnectedDataTable = connect(DataTable, { currentView: ViewStore });
ConnectedDataTable.type = 'table';

export default ConnectedDataTable;
