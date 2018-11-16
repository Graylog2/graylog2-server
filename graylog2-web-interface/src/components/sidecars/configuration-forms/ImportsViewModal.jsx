import PropTypes from 'prop-types';
import React from 'react';
import { Button, Modal, OverlayTrigger, Tooltip } from 'react-bootstrap';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';
import { PaginatedList, Spinner, Timestamp } from 'components/common';
import UserNotification from 'util/UserNotification';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

class ImportsViewModal extends React.Component {
  static propTypes = {
    onApply: PropTypes.func.isRequired,
  };

  static initialState = {
    uploads: undefined,
    totalUploads: 0,
    pagination: {
      page: 1,
    },
  };

  constructor(props) {
    super(props);
    this.state = ImportsViewModal.initialState;
  }

  PAGE_SIZE = 10;

  open = () => {
    this._loadUploads(this.state.pagination.page);
    this.uploadsModal.open();
  };

  hide = () => {
    this.uploadsModal.close();
  };

  _isLoading = () => {
    return !this.state.uploads;
  };

  _loadUploads = (page) => {
    CollectorConfigurationsActions.listUploads({ page: page })
      .then(
        (uploads) => {
          this.setState({ uploads: uploads.uploads, totalUploads: uploads.total });
        },
        (error) => {
          this.setState({ uploads: [], totalUploads: 0 });
          UserNotification.error(`Fetching uploads failed with error: ${error}`,
            'Could not get configuration uploads');
        },
      );
  };

  _onApplyButton = (selectedUpload) => {
    this.props.onApply(selectedUpload);
  };

  _formatUpload(upload) {
    const tooltip = <Tooltip id={`${upload.id}-status-tooltip`}>{upload.collector_id}</Tooltip>;

    return (
      <tr key={upload.id}>
        <td>
          <OverlayTrigger placement="top" overlay={tooltip} rootClose>
            <span>{upload.node_id}</span>
          </OverlayTrigger>
        </td>
        <td>{upload.collector_name}</td>
        <td><Timestamp dateTime={upload.created} format={'YYYY-MM-DD HH:mm:ss z'} /></td>
        <td>
          <Button bsStyle="info" bsSize="xsmall" onClick={e => this._onApplyButton(upload.rendered_configuration, e)}>
            Apply
          </Button>
        </td>
      </tr>
    );
  }

  render() {
    if (this._isLoading()) {
      return (
        <BootstrapModalWrapper ref={(c) => { this.uploadsModal = c; }}>
          <Modal.Header closeButton>
            <Modal.Title><span>Imports</span></Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Spinner />
          </Modal.Body>
          <Modal.Footer>
            <Button type="button" onClick={this.hide}>Close</Button>
          </Modal.Footer>
        </BootstrapModalWrapper>
      );
    }

    const pageSize = this.PAGE_SIZE;
    const { uploads, totalUploads } = this.state;
    const formattedUploads = uploads.map(upload => (this._formatUpload(upload)));

    return (
      <BootstrapModalWrapper ref={(c) => { this.uploadsModal = c; }}>
        <Modal.Header closeButton>
          <Modal.Title><span>Imports</span></Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <PaginatedList totalItems={totalUploads}
                         pageSize={pageSize}
                         showPageSizeSelect={false}
                         onChange={this._loadUploads}>
            <table className="table">
              <thead>
                <tr><th>Sidecar</th><th>Collector</th><th>Created</th><th>Action</th></tr>
              </thead>
              <tbody>
                {formattedUploads}
              </tbody>
            </table>
          </PaginatedList>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.hide}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default ImportsViewModal;
