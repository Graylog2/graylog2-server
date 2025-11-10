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

import { Col, Row } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';
import ProductLink from 'components/enterprise/ProductLink';
import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';

const GraylogEnterpriseHeader = styled.h2`
  margin-bottom: 10px;
`;

const AdvertisementSection = () => {
  const nodes = useStore(NodesStore);
  const productName = useProductName();
  const licensePlugin = PluginStore.exports('license');
  const ProductLinkComponent = licensePlugin[0]?.EnterpriseProductLink || ProductLink;
  const { clusterId } = nodes;

  return (
    <Row className="content">
      <Col md={6}>
        <GraylogEnterpriseHeader>{productName} Enterprise</GraylogEnterpriseHeader>
        <p>
          Designed to meet the needs of resource-constrained IT Operations and Software Engineering teams, {productName}{' '}
          Enterprise provides numerous productivity enhancements that will save you thousands of hours per year in
          collecting and analyzing log data to uncover the root cause of performance, outage, and error issues.
        </p>
        <ProductLinkComponent href="https://go2.graylog.org/request-graylog-operations" clusterId={clusterId}>
          Request now
        </ProductLinkComponent>
      </Col>
      <Col md={6}>
        <GraylogEnterpriseHeader>{productName} Security</GraylogEnterpriseHeader>
        <p>
          Extend {productName} Open’s capabilities for detecting, investigating, and responding to cybersecurity threats
          with security-specific dashboards and alerts, anomaly detection AI/ML engine, integrations with other security
          tools, SOAR capabilities, and numerous compliance reporting features.
        </p>
        <ProductLinkComponent
          href="https://go2.graylog.org/request-graylog-security"
          licenseSubject="/license/security"
          clusterId={clusterId}>
          Request now
        </ProductLinkComponent>
      </Col>
    </Row>
  );
};

export default AdvertisementSection;
