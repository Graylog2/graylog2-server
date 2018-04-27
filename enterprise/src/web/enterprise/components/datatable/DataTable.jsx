import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import expandRows from 'enterprise/logic/ExpandRows';
import connect from 'stores/connect';
import Field from 'enterprise/components/Field';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
import Value from 'enterprise/components/Value';
import DataTableEntry from './DataTableEntry';

class DataTable extends React.Component {
  static propTypes = {
    config: PropTypes.shape({
      data: PropTypes.oneOfType([
        PropTypes.arrayOf(PropTypes.object),
        PropTypes.shape({
          rows: PropTypes.arrayOf(PropTypes.object),
        }),
      ]),
      fields: PropTypes.arrayOf(PropTypes.string),
    }).isRequired,
  };

  _extractAllFieldnames = (data) => {
    const fieldNames = new Set();
    data.forEach((item) => {
      Object.keys(item).forEach(fieldName => fieldNames.add(fieldName));
    });

    return new Immutable.OrderedSet(fieldNames).sort();
  };

  _headerField = (field, span = 1) => (
    <th key={field} colSpan={span} style={{ left: '0px' }}>
      <Field name={field} queryId={this.props.currentView.selectedQuery}>{field}</Field>
    </th>
  );

  _headerFieldForValue = (field, value, span = 1) => (
    <th key={`${field}-${value}`} colSpan={span} style={{ left: '0px' }}>
      <Value field={field} value={value} queryId={this.props.currentView.selectedQuery}>{value}</Value>
    </th>
  );

  _spacer = (idx, span = 1) => <th colSpan={span} key={`spacer-${idx}`} style={{ left: '0px' }} />;

  _columnPivotHeaders = (columnPivots, rowPivots, series, actualColumnPivotValues) => {
    return (
      <tr>
        {rowPivots.map((_, idx) => this._spacer(idx))}
        {series.map((_, idx) => this._spacer(idx))}
        {columnPivots.map(field => this._headerField(field, actualColumnPivotValues.find(e => e.field === field).values.size))}
      </tr>
    );
  };

  _extractColumnPivotValues = (rows, columnFieldNames, rowFieldNames) => {
    const fieldsToSkip = rowFieldNames.slice(1);
    return columnFieldNames.map((fieldName) => {
      const expandedRows = rows.map((row) => {
        let ptr = [row];
        if (fieldsToSkip.length > 0) {
          fieldsToSkip.forEach((field) => {
            ptr = ptr.filter(r => r[field]).map(r => r[field]).flatten();
          });
        }

        return ptr;
      }).flatten();
      const presentFieldNames = expandedRows.filter(r => r[fieldName])
        .map(r => r[fieldName].map(f => f[fieldName]))
        .reduce((prev, cur) => prev.merge(cur), Immutable.OrderedSet());
      return { field: fieldName, values: presentFieldNames };
    });
  };

  render() {
    const { config, data } = this.props;
    const { columnPivots, rowPivots, series } = config;
    const rows = data[0] ? data[0].results : [];
    const rowFieldNames = rowPivots.map(pivot => pivot.field);
    const columnFieldNames = columnPivots.map(pivot => pivot.field);
    const fields = new Immutable.OrderedSet(rowFieldNames).merge(series);
    const sortedRows = expandRows(rowFieldNames.slice(), columnFieldNames.slice(), series, rows);
    const actualColumnPivotFields = this._extractColumnPivotValues(rows, columnFieldNames, rowFieldNames);
    const rowPivotFields = rowFieldNames.map(this._headerField);
    const seriesHeaders = series.map(this._headerField);
    const columnPivotFieldsHeaders = columnFieldNames.length > 0 && this._columnPivotHeaders(columnFieldNames, rowFieldNames, series, actualColumnPivotFields);
    const columnPivotValuesHeaders = actualColumnPivotFields.map(({ field, values }) => values.map(value => this._headerFieldForValue(field, value))).reduce((prev, cur) => prev.merge(cur), Immutable.OrderedSet());
    return (
      <div className="messages-container" style={{ overflow: 'auto', height: '100%' }}>
        <table className="table table-condensed messages">
          <thead>
            {columnPivotFieldsHeaders}
            <tr>
              {rowPivotFields}
              {seriesHeaders}
              {columnPivotValuesHeaders}
            </tr>
          </thead>
          {sortedRows.map((item, idx) => <DataTableEntry key={`datatableentry-${idx}`} fields={fields} item={item} columnPivots={columnFieldNames} columnPivotValues={actualColumnPivotFields} series={series}/>)}
        </table>
      </div>
    );
  }
}

export default connect(DataTable, { currentView: CurrentViewStore });
