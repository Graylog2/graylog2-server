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
import URI from 'urijs';

import ApiRoutes from 'routing/ApiRoutes';
import { Modal, Button } from 'components/graylog';
import { Icon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import URLUtils from 'util/URLUtils';
import StoreProvider from 'injection/StoreProvider';

const SessionStore = StoreProvider.getStore('Session');

class ContentPackDownloadControl extends React.Component {
  static propTypes = {
    contentPackId: PropTypes.string.isRequired,
    revision: PropTypes.number.isRequired,
  };

  constructor(props) {
    super(props);

    this._closeModal = this._closeModal.bind(this);
  }

  _getDownloadUrl() {
    const { contentPackId, revision } = this.props;

    const url = new URI(URLUtils.qualifyUrl(
      ApiRoutes.ContentPacksController.downloadRev(contentPackId, revision).url,
    ));

    if (URLUtils.areCredentialsInURLSupported()) {
      url
        .username(SessionStore.getSessionId())
        .password('session');
    }

    return url.toString();
  }

  _closeModal() {
    this.downloadModal.close();
  }

  open() {
    this.downloadModal.open();
  }

  render() {
    const infoText = (URLUtils.areCredentialsInURLSupported()
      ? 'Please right click the download link below and choose "Save Link As..." to download the JSON file.'
      : 'Please click the download link below. Your browser may ask for your username and password to '
      + 'download the JSON file.');

    return (
      <BootstrapModalWrapper ref={(node) => { this.downloadModal = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Download Content Pack</Modal.Title>
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
