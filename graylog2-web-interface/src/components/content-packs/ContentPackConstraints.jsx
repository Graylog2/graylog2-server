import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Badge } from 'react-bootstrap';
import ContentPackConstraintsStyle from './ContentPackConstraints.css';

class ContentPackConstraints extends React.Component {
  static propTypes = {
    constraints: PropTypes.array,
  };

  static defaultProps = {
    constraints: [],
  };

  static rowFormatter(item) {
    const fullfilledIcon = item.fullfilled ? <i className="fa fa-check" /> : <i className="fa fa-times" />;
    const fullfilledBg = item.fullfilled ? 'success' : 'failure';
    return (
      <tr key={item.id}>
        <td>{item.type}</td>
        <td>{item.name}</td>
        <td>{item.version}</td>
        <td><Badge className={fullfilledBg}>{fullfilledIcon}</Badge></td>
      </tr>
    );
  }

  render() {
    const headers = ['Name', 'Type', 'Version', 'Fullfilled'];
    return (
      <DataTable
        id="content-packs-constraints"
        headers={headers}
        headerCellFormatter={header => <th>{header}</th>}
        sortByKey="type"
        dataRowFormatter={ContentPackConstraints.rowFormatter}
        rows={this.props.constraints}
        filterKeys={[]}
      />);
  }
}

export default ContentPackConstraints;
