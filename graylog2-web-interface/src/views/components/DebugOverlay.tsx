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
// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { ViewStore } from 'views/stores/ViewStore';
import { SearchStore } from 'views/stores/SearchStore';
import connect from 'stores/connect';
import { Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import type { ViewStoreState } from 'views/stores/ViewStore';
import type { SearchStoreState } from 'views/stores/SearchStore';
import View from '../logic/views/View';

type Props = {
  currentView: ViewStoreState,
  onClose: () => void,
  searches: SearchStoreState,
  show: boolean,
};

const DebugOverlay = ({ currentView, searches, show, onClose }: Props) => (
  <BootstrapModalWrapper showModal={show} onHide={onClose}>
    <Modal.Header closeButton>
      <Modal.Title>Debug information</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <textarea disabled
                style={{ height: '80vh', width: '100%' }}
                value={JSON.stringify({ currentView, searches }, null, 2)} />
    </Modal.Body>
    <Modal.Footer>
      <Button type="button" onClick={() => onClose()} bsStyle="primary">Close</Button>
    </Modal.Footer>
  </BootstrapModalWrapper>
);

export default connect(DebugOverlay, { currentView: ViewStore, searches: SearchStore });
