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

import { Button, Col, Row, Panel, ButtonToolbar } from 'components/bootstrap';
import { Icon } from 'components/common';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import useDataNodes from 'components/datanode/hooks/useDataNodes';

type Props = {
  onStepComplete: () => void,
  onSkipCompatibilityCheck: () => void,
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

const MigrationHelpStep = ({ onStepComplete, onSkipCompatibilityCheck }: Props) => {
  const { data: dataNodes } = useDataNodes();

  return (
    <Row>
      <Col md={6}>
        <Headline>Migration to Datanode !</Headline>
        <p>
          It looks like you updated Graylog and want to configure a data node. Data nodes allow you to index and search through all the messages in your Graylog message database.
        </p>
        <p>
          Using this migration tool you can check the compatibility and follow the steps to migrate your exsisting Opensearch data to a Datanode.<br />
        </p>
        <p>Migrating to datanode require some step the are performed using the UI in this wizard, but it also require some additional step that should be performed on the OS, you current OS/ES cluster and you config files</p>
        <p>You can get more information on the Data node migration <DocumentationLink page="graylog-data-node" text="documentation" /></p>
        <br />
        <MigrationDatanodeList dataNodes={dataNodes} />
        <ButtonToolbar>
          <Button bsStyle="success" disabled={!dataNodes?.elements && dataNodes.elements.length > 0} onClick={() => onStepComplete()}>Check Compatibility</Button>
          <Button bsStyle="success" onClick={() => onSkipCompatibilityCheck()}>Skip Compatibility check</Button>
        </ButtonToolbar>
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
            <p>Please note that except for the servicename, I changed the cluster name and hostnames etc. to opensearch.
              In a regular setting, it would be the other way around and you would have to pull the elasticsearch names
              through the whole process into the DataNode.
            </p>
          </Panel.Body>
        </StyledHelpPanel>
      </Col>
    </Row>
  );
};

export default MigrationHelpStep;
