import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import { Button, ButtonToolbar, Modal } from 'react-bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import Spinner from 'components/common/Spinner';

import ContentPackInstallationView from 'components/content-packs/ContentPackInstallView';

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
    let showModalRef;
    const closeShowModal = () => {
      showModalRef.close();
    };

    const openShowModal = () => {
      showModalRef.open();
    };

    const showModal = (
      <BootstrapModalWrapper ref={(node) => { showModalRef = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>View Installation</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackInstallationView install={item} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={closeShowModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

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
              <Button bsStyle="info"
                      bsSize="small"
                      onClick={openShowModal}>
                View
              </Button>
              {showModal}
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
