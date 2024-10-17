import React from 'react';
import styled from 'styled-components';

import { Modal, Button } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

const VerboseMessage = styled.div`
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  max-height: 500px;
`;

type VerboseMessageModalProps = {
  showModal: boolean;
  onHide: (...args: any[]) => void;
  collectorName: string;
  collectorVerbose: string;
};

const VerboseMessageModal = ({
  showModal,
  onHide,
  collectorName,
  collectorVerbose,
}: VerboseMessageModalProps) => (
  <BootstrapModalWrapper showModal={showModal}
                         onHide={onHide}
                         bsSize="large">
    <Modal.Header closeButton>
      <Modal.Title><span>Error Details for <em>{collectorName}</em></span></Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <pre>
        <VerboseMessage>
          {collectorVerbose || '<no messages>'}
        </VerboseMessage>
      </pre>
    </Modal.Body>
    <Modal.Footer>
      <Button type="button" onClick={onHide}>Close</Button>
    </Modal.Footer>
  </BootstrapModalWrapper>
);

export default VerboseMessageModal;
