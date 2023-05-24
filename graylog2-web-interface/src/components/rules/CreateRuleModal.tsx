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

import { Button, BootstrapModalWrapper, Modal, Row, Col } from 'components/bootstrap';
import Routes from 'routing/Routes';

import { LinkContainer } from '../common/router';

type Props = {
  showModal: boolean,
  onClose: () => void,
};

const CreateRuleModal = ({ showModal, onClose }: Props) => (
  <BootstrapModalWrapper showModal={showModal}
                         onHide={onClose}
                         bsSize="large">
    <Modal.Header closeButton>
      <Modal.Title>Create Rule</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <Row>
        <Col md={6}>
          <LinkContainer to={`${Routes.SYSTEM.PIPELINES.RULE('new')}?rule_builder=true`}>
            <Button bsStyle="success">Rule Builder</Button>
          </LinkContainer>
        </Col>
        <Col md={6}>
          <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
            <Button bsStyle="success">Advanced</Button>
          </LinkContainer>
        </Col>
      </Row>
    </Modal.Body>
  </BootstrapModalWrapper>
);

export default CreateRuleModal;
