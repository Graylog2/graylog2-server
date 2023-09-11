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

import { Modal, Button } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import useAppSelector from 'stores/useAppSelector';

type Props = {
  onClose: () => void,
  show: boolean,
};

const DebugOverlay = ({ show, onClose }: Props) => {
  const fullState = useAppSelector((state) => state);

  return (
    <BootstrapModalWrapper showModal={show}
                           onHide={onClose}>
      <Modal.Header closeButton>
        <Modal.Title>Debug information</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <textarea disabled
                  style={{ height: '80vh', width: '100%' }}
                  value={JSON.stringify(fullState, null, 2)} />
      </Modal.Body>
      <Modal.Footer>
        <Button type="button" onClick={() => onClose()}>Close</Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

export default DebugOverlay;
