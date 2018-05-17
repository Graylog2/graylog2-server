import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Badge } from 'react-bootstrap';
import ContentPackConstraintsStyle from './ContentPackConstraints.css';

class ContentPackConstraints extends React.Component {
  static propTypes = {
    constraints: PropTypes.array,
    isFullFilled: PropTypes.bool,
  };

  static defaultProps = {
    constraints: [],
    isFullFilled: false,
  };

  _rowFormatter = (item) => {
    const fullfilledIcon = item.fullfilled || this.props.isFullFilled ? <i className="fa fa-check" /> : <i className="fa fa-times" />;
    const fullfilledBg = item.fullfilled || this.props.isFullFilled ? 'success' : 'failure';
    return (
      <tr key={item.id}>
        <td>{item.type}</td>
        <td>{item.version}</td>
        <td><Badge className={fullfilledBg}>{fullfilledIcon}</Badge></td>
      </tr>
    );
  };

  render() {
    const headers = ['Type', 'Version', 'Fullfilled'];
    return (
      <div>
        <h2>Constraints</h2>
        <br />
        <br />
        <DataTable
          id="content-packs-constraints"
          headers={headers}
          headerCellFormatter={header => <th>{header}</th>}
          sortByKey="type"
          dataRowFormatter={this._rowFormatter}
          rows={this.props.constraints}
          filterKeys={[]}
        />
      </div>
    );
  }
}

export default ContentPackConstraints;
