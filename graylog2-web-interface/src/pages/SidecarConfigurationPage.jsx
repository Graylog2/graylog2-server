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

import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import { DocumentTitle, PageHeader } from 'components/common';
import Routes from 'routing/Routes';
import ConfigurationListContainer from 'components/sidecars/configurations/ConfigurationListContainer';
import CollectorListContainer from 'components/sidecars/configurations/CollectorListContainer';

const SidecarConfigurationPage = () => (
  <DocumentTitle title="Collectors Configuration">
    <span>
      <PageHeader title="Collectors Configuration">
        <span>
          The Collector Sidecar runs next to your favourite log collector and configures it for you. Here you can
          manage the Sidecar configurations.
        </span>

        <span>
          Read more about the collector sidecar in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.COLLECTOR_SIDECAR} text="Graylog documentation" />.
        </span>

        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
            <Button bsStyle="info">Overview</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
            <Button bsStyle="info">Administration</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
            <Button bsStyle="info">Configuration</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <ConfigurationListContainer />
        </Col>
      </Row>
      <Row className="content">
        <Col md={12}>
          <CollectorListContainer />
        </Col>
      </Row>
    </span>
  </DocumentTitle>
);

export default SidecarConfigurationPage;
