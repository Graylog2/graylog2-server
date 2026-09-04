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
import * as React from 'react';

import { Alert, Button, Modal } from 'components/bootstrap';
import IncompatibleIndicesTable from 'components/indices/incompatible-indices/IncompatibleIndicesTable';

type Props = {
  show: boolean;
  onClose: () => void;
};

const IncompatibleIndicesModal = ({ show, onClose }: Props) => (
  <Modal show={show} onHide={onClose} bsSize="xl">
    <Modal.Header>
      <Modal.Title>Index Versions</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <Alert bsStyle="info">
        Any indices created with an incompatible, previous major version of OpenSearch need to be archived, deleted or
        reindexed before the search backend can be upgraded to the next major version.
      </Alert>
      <IncompatibleIndicesTable withoutURLParams />
    </Modal.Body>
    <Modal.Footer>
      <Button onClick={onClose}>Close</Button>
    </Modal.Footer>
  </Modal>
);

export default IncompatibleIndicesModal;
