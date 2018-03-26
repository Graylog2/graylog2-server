import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Button, DropdownButton, MenuItem } from 'react-bootstrap';

import '!style!css!./ContentPackVersions.css';

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
    this.rowFormatter = this.rowFormatter.bind(this);
    this.headerFormater = this.headerFormater.bind(this);
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
          <input type="radio" value={item.version} onChange={this.onChange} checked={this.state.selectedVersion === item.version} />
        </td>
        <td>{item.version}</td>
        <td className="text-right">
          <Button bsStyle="info" bsSize="small">View</Button>
          &nbsp;
          <DropdownButton id={`more-actions-${item.id}`} title="More Actions" bsSize="small" pullRight>
            <MenuItem>Remove</MenuItem>
            <MenuItem>Uninstall</MenuItem>
            <MenuItem>Create New Version</MenuItem>
            <MenuItem>Download</MenuItem>
          </DropdownButton>
        </td>
      </tr>
    );
  }

  headerFormater(header) {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    } else {
      return (<th>{header}</th>);
    }
  }

  render() {
    const headers = ['Select', 'Revision', 'Action'];
    const versions = this.props.versions.map((version) => { return { version: version }; });
    return (
      <DataTable
        id="content-packs-versions"
        headers={headers}
        headerCellFormatter={this.headerFormater}
        sortByKey="version"
        dataRowFormatter={this.rowFormatter}
        rows={versions}
        filterKeys={[]}
      />);
  }
}

export default ContentPackVersions;
