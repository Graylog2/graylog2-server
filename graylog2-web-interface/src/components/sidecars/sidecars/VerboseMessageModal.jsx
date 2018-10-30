import PropTypes from 'prop-types';
import React from 'react';
import { Button, Modal } from 'react-bootstrap';
import lodash from 'lodash';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

class VerboseMessageModal extends React.Component {

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
