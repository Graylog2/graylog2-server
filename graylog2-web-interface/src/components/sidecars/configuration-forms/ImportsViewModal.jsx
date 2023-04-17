/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { Button, Alert, Modal, Tooltip } from 'components/bootstrap';
import { OverlayTrigger, PaginatedList, Spinner, Timestamp, Icon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import UserNotification from 'util/UserNotification';
import { CollectorConfigurationsActions } from 'stores/sidecars/CollectorConfigurationsStore';

class ImportsViewModal extends React.Component {
  static propTypes = {
    onApply: PropTypes.func.isRequired,
    showModal: PropTypes.bool.isRequired,
    onHide: PropTypes.func.isRequired,
  };

  static initialState = {
    uploads: undefined,
    totalUploads: 0,
    pagination: {
      page: 1,
    },
  };

  PAGE_SIZE = 10;

  constructor(props) {
    super(props);
    this.state = ImportsViewModal.initialState;
  }

  componentDidUpdate() {
    if (this.props.showModal) {
      this._loadUploads(this.state.pagination.page);
    }
  }

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

  // eslint-disable-next-line class-methods-use-this
  _buildVariableName = (name) => {
    return `\${sidecar.${name}}`;
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
        <td><Timestamp dateTime={upload.created} /></td>
        <td>
          <Button bsStyle="info" bsSize="xsmall" onClick={() => this._onApplyButton(upload.rendered_configuration)}>
            Apply
          </Button>
        </td>
      </tr>
    );
  }

  _formatModalBody() {
    if (this._isLoading()) {
      return (
        <Spinner />
      );
    }

    const pageSize = this.PAGE_SIZE;
    const { uploads, totalUploads } = this.state;
    const formattedUploads = uploads.map((upload) => (this._formatUpload(upload)));

    if (totalUploads === 0) {
      return (
        <Alert bsStyle="info">
          <Icon name="info-circle" />&nbsp;
          There are no configuration uploads available. Please go to <strong>System -&gt; Collectors (legacy) -&gt; Details -&gt; Import Configuration</strong> and import your first configuration. You need at least Sidecar version 0.1.8 to make this feature available.
        </Alert>
      );
    }

    return (
      <PaginatedList totalItems={totalUploads}
                     pageSize={pageSize}
                     showPageSizeSelect={false}
                     onChange={this._loadUploads}
                     useQueryParameter={false}>
        <table className="table">
          <thead>
            <tr><th>Sidecar</th><th>Collector</th><th>Created</th><th>Action</th></tr>
          </thead>
          <tbody>
            {formattedUploads}
          </tbody>
        </table>
      </PaginatedList>
    );
  }

  render() {
    return (
      <BootstrapModalWrapper showModal={this.props.showModal}
                             onHide={this.props.onHide}
                             bsSize="large"
                             data-app-section="collector_configuration_form"
                             data-event-element="Migrate Imports from the old Collector system">
        <Modal.Header closeButton>
          <Modal.Title><span>Imports from the old Collector system</span></Modal.Title>
          Edit the imported configuration after pressing the Apply button by hand. Dynamic values like the node ID can be replaced with the variables system, e.g. <code>{this._buildVariableName('nodeId')}</code>
        </Modal.Header>
        <Modal.Body>
          {this._formatModalBody()}
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.props.onHide}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default ImportsViewModal;
