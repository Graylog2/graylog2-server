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
