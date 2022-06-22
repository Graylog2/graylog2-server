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
import styled, { css } from 'styled-components';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { NodesStore } from 'stores/nodes/NodesStore';
import PluginList from 'components/enterprise/PluginList';
import ProductLink from 'components/enterprise/ProductLink';
import { GraylogClusterOverview } from 'components/cluster';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { useStore } from 'stores/connect';
import EnterpriseProductLink from 'components/enterprise/EnterpriseProductLink';

const StyledHeader = styled.h2`
  margin-bottom: 10px;
`;

const BiggerFontSize = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
`);

const SecurityPage = () => {
  const nodes = useStore(NodesStore);

  if (!nodes) {
    return <Spinner />;
  }

  const { clusterId } = nodes;

  return (
    <DocumentTitle title="Try Graylog Security">
      <div>
        <PageHeader title="Try Graylog Security">
          {null}
          <span>
            Extend Graylog's capabilities for detecting, investigating, and responding to cybersecurity
            threats with security-specific dashboards and alerts, anomaly detection AI/ML engine, integrations with
            other security tools, SOAR capabilities, and numerous compliance reporting features. You can learn more
            about Graylog Security on the&nbsp;
            <EnterpriseProductLink href="https://www.graylog.org/products/security">
              product page
            </EnterpriseProductLink>.
          </span>
        </PageHeader>
        <GraylogClusterOverview layout="compact">
          <PluginList />
        </GraylogClusterOverview>
        <HideOnCloud>
          <Row className="content">
            <Col md={12}>
              <StyledHeader>Graylog Security</StyledHeader>
              <BiggerFontSize>
                <p>
                  Graylog Security is built on the Graylog platform. It combines the key features and functionality that set
                  us apart from the competition with SIEM, Security Analytics, & Anomaly Detection capabilities. IT security
                  teams get a superior cybersecurity platform designed to overcome legacy SIEM challenges. Your job becomes
                  easier. You can tackle critical activities faster. And you have the confidence and expertise to mitigate
                  risks caused by insider threats and credential-based attacks.
                </p>
                <ProductLink href="https://go2.graylog.org/request-graylog-security" clusterId={clusterId}>
                  Request now
                </ProductLink>
              </BiggerFontSize>
            </Col>
          </Row>
        </HideOnCloud>
      </div>
    </DocumentTitle>
  );
};

export default SecurityPage;
