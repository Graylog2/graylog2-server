import React from 'react';
import { Modal } from 'react-bootstrap';

import URLUtils from 'util/URLUtils';

const ServerUnavailablePage = React.createClass({
  render() {
    return (
      <Modal show>
        <Modal.Header>
          <Modal.Title>Server currently unavailable</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          The Graylog server is currenlly not available or returning an invalid response. Please make sure that
          the server running on <i>{URLUtils.qualifyUrl('')}</i> is working correctly.
        </Modal.Body>
      </Modal>
    );
  },
});

export default ServerUnavailablePage;
