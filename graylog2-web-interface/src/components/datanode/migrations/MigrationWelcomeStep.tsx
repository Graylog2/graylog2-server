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
import { Icon } from 'components/common';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationError from 'components/datanode/migrations/common/MigrationError';
import AppConfig from 'util/AppConfig';

import useIsElasticsearch from '../hooks/useIsElasticsearch';

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

const MigrationWelcomeStep = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const isElasticsearch = useIsElasticsearch();
  const isRemoteReindexingEnabled = AppConfig.isFeatureEnabled('remote_reindex_migration');

  return (
    <Row>
      <Col md={isRemoteReindexingEnabled ? 6 : 12}>
        {isElasticsearch && !isRemoteReindexingEnabled && (
          <Alert bsStyle="warning">
            Incompatible search backend. Please upgrade to OpenSearch to be able to use the migration wizard.
          </Alert>
        )}
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
        {!(isElasticsearch && !isRemoteReindexingEnabled) && (
          <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
        )}
      </Col>
      {isRemoteReindexingEnabled && (
        <Col md={6}>
          <StyledHelpPanel bsStyle="info">
            <Panel.Heading>
              <Panel.Title componentClass="h3"><Icon name="info" /> Methods for migration</Panel.Title>
            </Panel.Heading>
            <Panel.Body>
              <p>
                During the migration, you can choose between two options for migrating your existing ElasticSearch or OpenSearch data to the data nodes.
                You should choose between them based on your individual prerequisites and requirements.
              </p>
              <p>
                If you are already running <code>OpenSearch (1.x or 2.x)</code> as your search backend, you can choose <code>in-place migration</code>.
                In this migration scenario, the data node’s OpenSearch will use the existing data directory of OpenSearch to serve all data previously available in your existing OpenSearch.
                This is the recommended method if you want to quickly migrate to data node.
              </p>
              <p>
                If you want to selectively migrate data (e.g. if you use your search backend non-exclusively for Graylog),
                you should choose the <code>remote reindexing migration</code>.
                In this scenario, all data will be copied from your existing search backend to data node’s OpenSearch.
                Depending on your setup, this can take some time and imposes additional disk space for the copied data to be available.
                During the remote reindexing, Graylog is ingesting data into data node and can be used,
                but will only serve the data from the old search backend as it becomes available.
              </p>
              <p>
                If you are running <code>ElasticSearch</code> as your search backend <code>remote reindexing migration</code> will automatically be chosen.
              </p>
              <p>
                If you don’t plan to migrate any existing data or only want to migrate a small subset of data
                we recommend you choose the remote reindexing migration and either skip the data migration or choose only the selected indices for migration.
              </p>
            </Panel.Body>
          </StyledHelpPanel>
        </Col>
      )}
    </Row>
  );
};

export default MigrationWelcomeStep;
