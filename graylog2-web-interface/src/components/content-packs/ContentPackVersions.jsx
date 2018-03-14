import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Badge } from 'react-bootstrap';

class ContentPackVersions extends React.Component {
  static propTypes = {
    versions: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func,
  };

  static defaultProps = {
    versions: [],
    onChange: () => {},
  };

  constructor(props) {
    super(props);
    this.state = { selectedVersion: this.props.versions[0] };

    this.onChange = this.onChange.bind(this);
  }

  onChange(event) {
    this.setState({
      selectedVersion: event.target.value,
    });
    this.props.onChange(event.target.value);
  }

  rowFormatter(item) {
    return (
      <tr key={item}>
        <td>
          <input type="radio" value={item} onChange={this.onChange} checked={this.state.selectedVersion === item} />
        </td>
        <td>{item}</td>
        <td />
      </tr>
    );
  }

  render() {
    const headers = ['Select', 'Revision', 'Action'];
    return (
      <DataTable
        id="content-packs-versions"
        headers={headers}
        headerCellFormatter={header => <th>{header}</th>}
        sortByKey="type"
        dataRowFormatter={this.rowFormatter}
        rows={this.props.versions}
        filterKeys={[]}
      />);
  }
}

export default ContentPackVersions;
