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
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationError from 'components/datanode/migrations/common/MigrationError';

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

const MigrationWelcomeStep = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => (
  <Row>
    <Col md={6}>
      <MigrationError errorMessage={currentStep.error_message} />
      <Headline>Data Nodes Migration</Headline>
      <p>
        The Graylog Data Node is a management component designed to configure and optimize OpenSearch for use with Graylog, reducing administrative overhead and simplifying future updates.
      </p>
      <p>Deployments earlier than v5.2 or that opted to not install with a Data Node will need to migrate the message databases to Data Nodes.</p>
      <p>
        This migration tool will check the compatibility of your components and guide you through to migrate your existing OpenSearch data to a Data Node.<br />
      </p>
      <p>Migrating to Data Node will require some steps to be performed on the OS, within your current OS/ES cluster, and in your configuration files.</p>
      <p>You can get more information on the Data Node migration <DocumentationLink page="graylog-data-node" text="documentation" />.</p>
      <br />
      <MigrationDatanodeList />
      <MigrationStepTriggerButtonToolbar nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
    </Col>
    <Col md={6}>
      <StyledHelpPanel bsStyle="info">
        <Panel.Heading>
          <Panel.Title componentClass="h3"><Icon name="info" /> Migrating Elasticsearch 7.10</Panel.Title>
        </Panel.Heading>
        <Panel.Body>
          <p>Migration from <code>Elasticsearch 7.10</code> needs an additional step. ES 7.10 does not understand JWT
            authentication.
            So you will need to first migrate to OpenSearch before running the update of the security information. Please find more information in our <DocumentationLink page="graylog-data-node" text="documentation" />.
          </p>
          <h5>Migrating an OpenSearch Cluster</h5>
          <p>
            Depending on how you secured your existing cluster, some preliminary changes are needed to the security configuration. We use JWT authentication between Graylog and OpenSearch. If you want to perform an In-Place migration of your existing cluster into the Data Node, you have to manually include JWT authentication to your existing OpenSearch cluster prior to running the migration wizard.
          </p>
          <p>
            Enable JWT authentication in <code>opensearch-security/config.yml</code> (section <code>jwt_auth_domain</code>, <code>http_enabled: true</code>, <code>transport_enabled: true</code>)
            Add the signing key by converting the <code>GRAYLOG_PASSWORD_SECRET</code> to <code>base64</code> e.g. by doing <code>echo `&quot;`The password secret you chose`&quot;` | base64</code> and put it into the <code>signing_key</code> line
          </p>
          <h5>Usage of certificates</h5>
          <p>
            If your existing cluster uses certificates, by default these will get replaced with the Graylog CA and automatically generated certificates during provisioning of the Data Nodes. If you have to reuse your own certificates, please read the <DocumentationLink page="graylog-data-node" text="documentation" /> on how to include your own CA/certificates in Graylog/DataNode
          </p>
        </Panel.Body>
      </StyledHelpPanel>
    </Col>
  </Row>
);

export default MigrationWelcomeStep;
