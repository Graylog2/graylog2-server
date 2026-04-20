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

import { Col, Row, Panel, Alert } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationError from 'components/datanode/migrations/common/MigrationError';
import useProductName from 'brand-customization/useProductName';

import useIsElasticsearch from '../hooks/useIsElasticsearch';

const Headline = styled.h2`
  margin-top: 5px;
  margin-bottom: 10px;
`;

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(
  ({ bsStyle = 'default', theme }) => css`
    &.panel {
      background-color: ${theme.colors.global.contentBackground};

      .panel-heading {
        color: ${theme.colors.variant.darker[bsStyle]};
      }
    }
  `,
);

const MigrationWelcomeStep = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const isElasticsearch = useIsElasticsearch();
  const productName = useProductName();

  return (
    <Row>
      <Col md={12}>
        {isElasticsearch && (
          <Alert bsStyle="warning">
            Incompatible search backend. Please upgrade to OpenSearch to be able to use the migration wizard.
          </Alert>
        )}
        <MigrationError errorMessage={currentStep.error_message} />
        <Headline>Data Nodes Migration</Headline>
        <p>
          The {productName} Data Node is a management component designed to configure and optimize OpenSearch for use
          with {productName}, reducing administrative overhead and simplifying future updates.
        </p>
        <p>
          Deployments earlier than v5.2 or that opted to not install with a Data Node will need to migrate the message
          databases to Data Nodes.
        </p>
        <p>
          This migration tool will check the compatibility of your components and guide you through to migrate your
          existing OpenSearch data to a Data Node.
          <br />
        </p>
        <p>
          Migrating to Data Node will require some steps to be performed on the OS, within your current OS/ES cluster,
          and in your configuration files.
        </p>
        <p>
          You can get more information on the Data Node migration{' '}
          <DocumentationLink page="graylog-data-node" text="documentation" />.
        </p>
        <br />
        <MigrationDatanodeList />
        {!isElasticsearch && (
          <MigrationStepTriggerButtonToolbar
            hidden={hideActions}
            nextSteps={currentStep.next_steps}
            onTriggerStep={onTriggerStep}
          />
        )}
      </Col>
    </Row>
  );
};

export default MigrationWelcomeStep;
