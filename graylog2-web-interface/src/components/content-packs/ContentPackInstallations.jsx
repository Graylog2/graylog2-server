import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Button, DropdownButton, MenuItem } from 'react-bootstrap';

class ContentPackInstallations extends React.Component {
  static propTypes = {
    installations: PropTypes.arrayOf(PropTypes.string),
  };

  static defaultProps = {
    installations: [],
  };

  constructor(props) {
    super(props);

    this.rowFormatter = this.rowFormatter.bind(this);
    this.headerFormater = this.headerFormater.bind(this);
  }

  rowFormatter(item) {
    return (
      <tr key={item}>
        <td>
          {item.comment}
        </td>
        <td>{item.version}</td>
        <td className="text-right">
          <Button bsStyle="info" bsSize="small">Install</Button>
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
    const headers = ['Comment', 'Version', 'Action'];
    return (
      <DataTable
        id="content-packs-versions"
        headers={headers}
        headerCellFormatter={this.headerFormater}
        sortByKey="comment"
        dataRowFormatter={this.rowFormatter}
        rows={this.props.installations}
        filterKeys={[]}
      />);
  }
}

export default ContentPackInstallations;
