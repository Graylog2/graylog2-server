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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import { GraylogClusterOverview } from 'components/cluster';
import PluginList from 'components/enterprise/PluginList';
import EnterpriseProductLink from 'components/enterprise/EnterpriseProductLink';
import ProductLink from 'components/enterprise/ProductLink';
import HideOnCloud from 'util/conditional/HideOnCloud';

const GraylogEnterpriseHeader = styled.h2`
  margin-bottom: 10px;
`;

const EnterprisePage = () => {
  const nodes = useStore(NodesStore);
  const licensePlugin = PluginStore.exports('license');
  const ProductLinkComponent = licensePlugin[0]?.EnterpriseProductLink || ProductLink;

  if (!nodes) {
    return <Spinner />;
  }

  const { clusterId } = nodes;

  return (
    <DocumentTitle title="Try Graylog Enterprise">
      <div>
        <PageHeader title="Try Graylog Enterprise">
          <span>
            Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
            about Graylog Enterprise on the <EnterpriseProductLink>product page</EnterpriseProductLink>.
          </span>
        </PageHeader>

        <GraylogClusterOverview layout="compact">
          <PluginList />
        </GraylogClusterOverview>
        <HideOnCloud>
          <Row className="content">
            <Col md={6}>
              <GraylogEnterpriseHeader>Graylog Operations</GraylogEnterpriseHeader>
              <p>
                Designed to meet the needs of resource-constrained IT Operations and Software Engineering teams,
                Graylog Operations provides numerous productivity enhancements that will save you thousands of
                hours per year in collecting and analyzing log data to uncover the root cause of performance,
                outage, and error issues.
              </p>
              <ProductLinkComponent href="https://go2.graylog.org/request-graylog-operations" clusterId={clusterId}>
                Request now
              </ProductLinkComponent>
            </Col>
            <Col md={6}>
              <GraylogEnterpriseHeader>Graylog Security</GraylogEnterpriseHeader>
              <p>
                Extend Graylog Open’s capabilities for detecting, investigating, and responding to cybersecurity
                threats with security-specific dashboards and alerts, anomaly detection AI/ML engine,
                integrations with other security tools, SOAR capabilities, and numerous compliance reporting
                features.
              </p>
              <ProductLinkComponent href="https://go2.graylog.org/request-graylog-security" licenseSubject="/license/security" clusterId={clusterId}>
                Request now
              </ProductLinkComponent>
            </Col>
          </Row>
        </HideOnCloud>
      </div>
    </DocumentTitle>
  );
};

export default EnterprisePage;
