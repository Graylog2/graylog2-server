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

import { Button, BootstrapModalWrapper, Modal, Row, Col, Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { Select, SourceCodeEditor } from 'components/common';

import { LinkContainer } from '../common/router';

const SubTitle = styled.label`
  color: #aaa;
  margin-top: 10px;
`;

const StyledInput = styled(Input)`
  margin-top: 5px;
  background-color: white;
`;

const StyledCol = styled(Col)`
  border-right: solid 1px;
`;

const RuleBuilderRow = styled(Row)`
  height: 155px;
`;

const RULE_TEMPLATE = `rule "function howto"
when
  has_field("transaction_date")
then
  set_field("transaction_year", ${new Date().getFullYear()});
end`;

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
        <StyledCol md={6}>
          <label htmlFor="rule_builder">Rule Builder</label>
          <RuleBuilderRow>
            <Col md={6}>
              <SubTitle htmlFor="rule_builder_conditions">Conditions</SubTitle>
              <Select value="has_field" clearable={false} onChange={() => {}} options={[]} />
              <StyledInput value="transaction_date" type="text" />
            </Col>
            <Col md={6}>
              <SubTitle htmlFor="rule_builder_actions">Actions</SubTitle>
              <Select value="set_field" clearable={false} onChange={() => {}} options={[]} />
              <StyledInput value="transaction_date" type="text" />
              <StyledInput value={new Date().getFullYear()} type="text" />
            </Col>
          </RuleBuilderRow>
          <br />
          <div>
            It can be converted into <i>Source Code</i> at any moment.
          </div>
          <br />
          <LinkContainer to={`${Routes.SYSTEM.PIPELINES.RULE('new')}?rule_builder=true`}>
            <Button bsStyle="success">Use Rule Builder</Button>
          </LinkContainer>
        </StyledCol>
        <Col md={6}>
          <label htmlFor="source_code">Source Code</label>
          {/* @ts-ignore */}
          <SourceCodeEditor mode="pipeline"
                            value={RULE_TEMPLATE}
                            resizable={false}
                            height={120}
                            readOnly />
          <br />
          <div>
            It can <b>not</b> be converted into <i>Rule Builder</i>.
          </div>
          <br />
          <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
            <Button bsStyle="success">Use Source Code</Button>
          </LinkContainer>
        </Col>
      </Row>
    </Modal.Body>
  </BootstrapModalWrapper>
);

export default CreateRuleModal;
