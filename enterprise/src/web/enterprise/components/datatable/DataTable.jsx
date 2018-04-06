import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import expandRows from 'enterprise/logic/ExpandRows';
import connect from 'stores/connect';
import Field from 'enterprise/components/Field';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';
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

  render() {
    const { config, data } = this.props;
    const { rowPivots, series } = config;
    const rows = data[0] ? data[0].results : [];
    const fields = new Immutable.OrderedSet(rowPivots).merge(this._extractAllFieldnames(rows));
    const sortedRows = expandRows(rowPivots.slice(), series, rows);
    const { selectedQuery } = this.props.currentView;
    return (
      <div className="messages-container">
        <table className="table table-condensed messages">
          <thead>
            <tr>
              {fields.toSeq().map((field) => {
                return (
                  <th key={field} style={{ left: '0px' }}>
                    <Field interactive name={field} queryId={selectedQuery} >{field}</Field>
                  </th>
                );
              })}
            </tr>
          </thead>
          {sortedRows.map((item, idx) => <DataTableEntry key={`datatableentry-${idx}`} fields={fields} item={item} />)}
        </table>
      </div>
    );
  }
}

export default connect(DataTable, { currentView: CurrentViewStore });
