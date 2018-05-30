import React from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import Immutable from 'immutable';
import { flatten, get, isEqual, last, uniqWith } from 'lodash';

import expandRows from 'enterprise/logic/ExpandRows';
import connect from 'stores/connect';
import Field from 'enterprise/components/Field';
import Value from 'enterprise/components/Value';
import { ViewStore } from 'enterprise/stores/ViewStore';
import FieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import DataTableEntry from './DataTableEntry';
import { AggregationType } from '../aggregationbuilder/AggregationBuilderPropTypes';

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
        if (get(last(prev), 'key') === cur.key) {
          const lastItem = last(prev);
          const remainder = prev.slice(0, -1);
          const newLastItem = Object.assign({}, lastItem, { count: lastItem.count + 1 });
          return [].concat(remainder, [newLastItem]);
        }
        return [].concat(prev, [cur]);
      }, []);
      return (
        <tr>
          {this._spacer(1, offset)}
          {actualValuesWithoutDuplicates.map(value => this._headerFieldForValue(columnPivot, value.key, value.count * series.length, value.path))}
        </tr>
      );
    });
  };

  _extractColumnPivotValues = (rows) => {
    return uniqWith(flatten(rows.map(({ values }) => values))
      .filter(({ rollup }) => !rollup)
      .map(({ key }) => key.slice(0, -1)), isEqual);
  };

  render() {
    const { config, data } = this.props;
    const { columnPivots, rowPivots, series } = config;
    const rows = data || [];

    const rowFieldNames = rowPivots.map(pivot => pivot.field);
    const columnFieldNames = columnPivots.map(pivot => pivot.field);
    const fields = new Immutable.OrderedSet(rowFieldNames).merge(series);

    const expandedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), series, rows);

    const actualColumnPivotFields = this._extractColumnPivotValues(rows, columnFieldNames);
    const rowPivotFields = rowFieldNames.map(this._headerField);

    const columnPivotFieldsHeaders = this._columnPivotHeaders(columnFieldNames, actualColumnPivotFields, series, rowFieldNames.length + series.length);
    return (
      <div className="messages-container" style={{ overflow: 'auto', height: '100%' }}>
        <table className="table table-condensed messages">
          <thead>
            {columnPivotFieldsHeaders}
            <tr>
              {rowPivotFields}
              {series.map(this._headerField)}
              {actualColumnPivotFields.map(key => series.map(s => this._headerField(s, key.join('-'))))}
            </tr>
          </thead>
          {expandedRows.map((item, idx) => <DataTableEntry key={`datatableentry-${idx}`} fields={fields} item={item} columnPivots={columnFieldNames} columnPivotValues={actualColumnPivotFields} series={series}/>)}
        </table>
      </div>
    );
  }
}

export default connect(DataTable, { currentView: ViewStore });
