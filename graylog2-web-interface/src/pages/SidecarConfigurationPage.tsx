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

import { Col, Row } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader } from 'components/common';
import ConfigurationListContainer from 'components/sidecars/configurations/ConfigurationListContainer';
import CollectorListContainer from 'components/sidecars/configurations/CollectorListContainer';
import SidecarsPageNavigation from 'components/sidecars/common/SidecarsPageNavigation';

const SidecarConfigurationPage = () => (
  <DocumentTitle title="Collectors Configuration">
    <SidecarsPageNavigation />
    <PageHeader title="Collectors Configuration"
                documentationLink={{
                  title: 'Sidecar documentation',
                  path: DocsHelper.PAGES.COLLECTOR_SIDECAR,
                }}>
      <span>
        The Collector Sidecar runs next to your favourite log collector and configures it for you. Here you can
        manage the Sidecar configurations.
      </span>
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

  </DocumentTitle>
);

export default SidecarConfigurationPage;
