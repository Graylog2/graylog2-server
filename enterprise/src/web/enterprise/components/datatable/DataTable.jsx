import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import naturalSort from 'javascript-natural-sort';

import expandRows from 'enterprise/logic/ExpandRows';
import DataTableEntry from './DataTableEntry';
import Field from '../Field';

const DataTable = React.createClass({
  propTypes: {
    config: PropTypes.shape({
      data: PropTypes.oneOfType([
        PropTypes.arrayOf(PropTypes.object),
        PropTypes.shape({
          rows: PropTypes.arrayOf(PropTypes.object),
        }),
      ]),
      fields: PropTypes.arrayOf(PropTypes.string),
    }).isRequired,
  },
  _extractAllFieldnames(data) {
    const fieldNames = new Set();
    data.forEach((item) => {
      Object.keys(item).forEach(fieldName => fieldNames.add(fieldName));
    });

    return new Immutable.OrderedSet(fieldNames).sort();
  },
  render() {
    const { config, data } = this.props;
    const { rowPivots, series } = config;
    const rows = data[0] ? data[0].results : [];
    const fields = new Immutable.OrderedSet(rowPivots).merge(series.filter(s => s !== 'count()')).merge(this._extractAllFieldnames(rows));
    const sortedRows = expandRows(rowPivots.slice(), series, rows);
    return (
      <div className="messages-container">
        <table className="table table-condensed messages">
          <thead>
            <tr>
              {fields.toSeq().map((field) => {
                return (
                  <th key={field} style={{ left: '0px' }}>
                    <Field interactive name={field} queryId="FIXME" >{field}</Field>
                  </th>
                );
              })}
            </tr>
          </thead>
          {sortedRows.map((item, idx) => <DataTableEntry key={`datatableentry-${idx}`} fields={fields} item={item} />)}
        </table>
      </div>
    );
  },
});

export default DataTable;
