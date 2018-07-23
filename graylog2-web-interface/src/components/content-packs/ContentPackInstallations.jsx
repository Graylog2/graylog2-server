import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Button, ButtonToolbar } from 'react-bootstrap';
import Spinner from 'components/common/Spinner';

class ContentPackInstallations extends React.Component {
  static propTypes = {
    installations: PropTypes.arrayOf(PropTypes.string),
    onUninstall: PropTypes.func,
  };

  static defaultProps = {
    installations: [],
    onUninstall: () => {},
  };

  rowFormatter = (item) => {
    return (
      <tr key={item}>
        <td>
          {item.comment}
        </td>
        <td>{item.content_pack_revision}</td>
        <td>
          <div className="pull-right">
            <ButtonToolbar>
              <Button bsStyle="primary"
                      bsSize="small"
                      onClick={() => { this.props.onUninstall(item.content_pack_id, item._id); }}>
                Uninstall
              </Button>
              <Button bsStyle="info" bsSize="small">View</Button>
            </ButtonToolbar>
          </div>
        </td>
      </tr>
    );
  };

  headerFormater = (header) => {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    }
    return (<th>{header}</th>);
  };

  render() {
    if (!this.props.installations) {
      return (<Spinner />);
    }

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
