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

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Modal, Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

class ContentPackDownloadControl extends React.Component {
  static propTypes = {
    contentPackId: PropTypes.string.isRequired,
    revision: PropTypes.number.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      showDownloadModal: false,
    };

    this._closeModal = this._closeModal.bind(this);
  }

  _getDownloadUrl() {
    const { contentPackId, revision } = this.props;

    return qualifyUrl(ApiRoutes.ContentPacksController.downloadRev(contentPackId, revision).url);
  }

  _closeModal() {
    this.setState({ showDownloadModal: false });
  }

  // eslint-disable-next-line react/no-unused-class-component-methods
  open() {
    this.setState({ showDownloadModal: true });
  }

  render() {
    const infoText = 'Please right click the download link below and choose "Save Link As..." to download the JSON file.';
    const modalTitle = 'Download Content Pack';

    return (
      <BootstrapModalWrapper showModal={this.state.showDownloadModal}
                             onHide={this._closeModal}
                             bsSize="large"
                             data-app-section="content_pack_actions"
                             data-event-element={modalTitle}>
        <Modal.Header closeButton>
          <Modal.Title>{modalTitle}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>{infoText}</p>
          <p>
            <a href={this._getDownloadUrl()} target="_blank" rel="noopener noreferrer">
              <Icon name="cloud-download-alt" />{' '}Download
            </a>
          </p>
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this._closeModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default ContentPackDownloadControl;
