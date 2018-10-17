import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Badge } from 'react-bootstrap';
import './ContentPackConstraints.css';

class ContentPackConstraints extends React.Component {
  static propTypes = {
    constraints: PropTypes.array,
    isFulfilled: PropTypes.bool,
  };

  static defaultProps = {
    constraints: [],
    isFulfilled: false,
  };

  _rowFormatter = (item) => {
    const constraint = item.constraint || item;
    const fulfilledIcon = item.fulfilled || this.props.isFulfilled ? <i className="fa fa-check" /> : <i className="fa fa-times" />;
    const fulfilledBg = item.fulfilled || this.props.isFulfilled ? 'success' : 'failure';
    const name = constraint.type === 'server-version' ? 'Graylog' : constraint.plugin;
    return (
      <tr key={constraint.id}>
        <td>{name}</td>
        <td>{constraint.type}</td>
        <td>{constraint.version}</td>
        <td><Badge className={fulfilledBg}>{fulfilledIcon}</Badge></td>
      </tr>
    );
  };

  render() {
    const headers = ['Name', 'Type', 'Version', 'Fulfilled'];
    const constraints = this.props.constraints.map((constraint) => {
      const newConstraint = constraint.constraint || constraint;
      newConstraint.fulfilled = constraint.fulfilled;
      return newConstraint;
    });
    return (
      <div>
        <h2>Constraints</h2>
        <br />
        <br />
        <DataTable
          id="content-packs-constraints"
          headers={headers}
          headerCellFormatter={header => <th>{header}</th>}
          sortBy={(row) => { return row.constraint ? row.constraint.type : row.type; }}
          dataRowFormatter={this._rowFormatter}
          rows={constraints}
          filterKeys={[]}
        />
      </div>
    );
  }
}

export default ContentPackConstraints;
