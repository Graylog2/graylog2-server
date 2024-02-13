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
import styled from 'styled-components';

import { Col, Panel, Row } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import { Icon } from 'components/common';
import { StyledPanel } from 'components/datanode/migrations/MigrationWelcomeStep';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';

const StyledHelpPanel = styled(StyledPanel)`
  margin-top: 30px;
`;

const Welcome = ({ nextSteps, onTriggerStep }: MigrationStepComponentProps) => {
  const { data: dataNodes } = useDataNodes();

  return (
    <>
      <Row>
        <Col md={6}>
          <h3>Welcome</h3>
          <p>Using the rolling upgrade will allow you to move the data node by using the data folder of your existing cluster to the datanode cluster.</p>
          <p>To start please install Data node on every OS/ES node from you previous setup. You can find more information on how to download and install the data node  <DocumentationLink page="graylog-data-node" text="here" />.</p>
          <MigrationDatanodeList dataNodes={dataNodes} />
        </Col>
        <Col md={6}>
          <StyledHelpPanel bsStyle="warning">
            <Panel.Heading>
              <Panel.Title componentClass="h3"><Icon name="exclamation-triangle" /> Journal size</Panel.Title>
            </Panel.Heading>
            <Panel.Body>
              <p>Please note that during migration you will have to stop processing on your graylog node, this will result in the journal growing in size.
                Therefore you will have to increase your journal volume size during the Journal size downsize step or earlier.
              </p>
            </Panel.Body>
          </StyledHelpPanel>
        </Col>
      </Row>
      <MigrationStepTriggerButtonToolbar disabled={!dataNodes} nextSteps={nextSteps} onTriggerStep={onTriggerStep} />
    </>
  );
};

export default Welcome;
