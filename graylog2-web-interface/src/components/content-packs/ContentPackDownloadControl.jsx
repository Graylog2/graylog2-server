import PropTypes from 'prop-types';
import React from 'react';
import URI from 'urijs';

import ApiRoutes from 'routing/ApiRoutes';
import { Button, Modal } from 'react-bootstrap';
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
    const url = new URI(URLUtils.qualifyUrl(
      ApiRoutes.ContentPacksController.getRev(this.props.contentPackId, this.props.revision).url,
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
    const infoText = (URLUtils.areCredentialsInURLSupported() ?
      'Please right click the download link below and choose "Save Link As..." to download the JSON file.' :
      'Please click the download link below. Your browser may ask for your username and password to ' +
      'download the JSON file.');
    return (
      <BootstrapModalWrapper ref={(node) => { this.downloadModal = node; }}>
        <Modal.Header closeButton>
          <Modal.Title>Export search results as CSV</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>{infoText}</p>
          <p>
            <a href={this._getDownloadUrl()} target="_blank">
              <i className="fa fa-cloud-download" />&nbsp;
              Download
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
