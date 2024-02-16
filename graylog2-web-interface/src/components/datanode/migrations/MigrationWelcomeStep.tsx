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
import styled, { css } from 'styled-components';

import { Col, Row, Panel } from 'components/bootstrap';
import { Icon } from 'components/common';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationActions, OnTriggerStepFunction } from 'components/datanode/Types';

type Props = {
  nextSteps: Array<MigrationActions>,
  onTriggerStep: OnTriggerStepFunction,
};

const Headline = styled.h2`
  margin-top: 5px;
  margin-bottom: 10px;
`;

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(({ bsStyle = 'default', theme }) => css`
  &.panel {
    background-color: ${theme.colors.global.contentBackground};
    .panel-heading {
      color: ${theme.colors.variant.darker[bsStyle]};
    }
  }
`);

const StyledHelpPanel = styled(StyledPanel)`
  margin-top: 30px;
`;

const MigrationWelcomeStep = ({ nextSteps, onTriggerStep }: Props) => (
  <Row>
    <Col md={6}>
      <Headline>Migration to Data node !</Headline>
      <p>
        It looks like you updated Graylog and want to configure a data node. Data nodes allow you to index and search through all the messages in your Graylog message database.
      </p>
      <p>
        Using this migration tool you can check the compatibility and follow the steps to migrate your exsisting Opensearch data to a Data node.<br />
      </p>
      <p>Migrating to data node require some step the are performed using the UI in this wizard, but it also require some additional step that should be performed on the OS, you current OS/ES cluster and you config files</p>
      <p>You can get more information on the Data node migration <DocumentationLink page="graylog-data-node" text="documentation" /></p>
      <br />
      <MigrationDatanodeList />
      <MigrationStepTriggerButtonToolbar nextSteps={nextSteps} onTriggerStep={onTriggerStep} />
    </Col>
    <Col md={6}>
      <StyledHelpPanel bsStyle="info">
        <Panel.Heading>
          <Panel.Title componentClass="h3"><Icon name="info-circle" /> Migrating Elasticsearch 7.10</Panel.Title>
        </Panel.Heading>
        <Panel.Body>
          <p>Migration from <code>Elasticsearch 7.10</code> needs an additional step. ES 7.10 does not understand JWT
            authentication.
            So you have to first migrate to OpenSearch before running the update of the security information. Look at
            the supplied <code>es710-docker-compose.yml</code> as an example.
          </p>
        </Panel.Body>
      </StyledHelpPanel>
    </Col>
  </Row>
);

export default MigrationWelcomeStep;
