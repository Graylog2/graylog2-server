import PropTypes from 'prop-types';
import React from 'react';

import { Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

class VerboseMessageModal extends React.Component {
  static propTypes = {
    collectorName: PropTypes.string.isRequired,
    collectorVerbose: PropTypes.string.isRequired,
  };

  open = () => {
    this.sourceModal.open();
  };

  hide = () => {
    this.sourceModal.close();
  };

  render() {
    return (
      <BootstrapModalWrapper ref={(c) => { this.sourceModal = c; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title><span>Error Details for <em>{this.props.collectorName}</em></span></Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div className="configuration">
            <pre>
              {this.props.collectorVerbose || '<no messages>'}
            </pre>
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button type="button" onClick={this.hide}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  }
}

export default VerboseMessageModal;
