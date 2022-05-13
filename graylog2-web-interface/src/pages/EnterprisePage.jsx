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
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import styled, { css } from 'styled-components';

import { DocumentTitle, PageHeader } from 'components/common';
import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { GraylogClusterOverview } from 'components/cluster';
import PluginList from 'components/enterprise/PluginList';
import HideOnCloud from 'util/conditional/HideOnCloud';

const EnterpriseProductLink = ({ children }) => {
  return (
    <a href="https://www.graylog.org/products/enterprise"
       rel="noopener noreferrer"
       target="_blank">
      {children}
    </a>
  );
};

const ProductLink = ({ href, clusterId, children }) => {
  let hrefWithParam = href;

  if (clusterId) {
    hrefWithParam = `${hrefWithParam}?cluster_id=${clusterId}`;
  }

  return (
    <ButtonToolbar>
      <Button type="link"
              target="_blank"
              rel="noopener noreferrer"
              href={hrefWithParam}
              bsStyle="primary">
        {children}
      </Button>
    </ButtonToolbar>
  );
};

EnterpriseProductLink.propTypes = {
  children: PropTypes.node,
};

EnterpriseProductLink.defaultProps = {
  children: null,
};

const BiggerFontSize = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
`);

const GraylogEnterpriseHeader = styled.h2`
  margin-bottom: 10px;
`;

const EnterprisePage = createReactClass({
  displayName: 'EnterprisePage',

  render() {
    // TODO: Actually request and set clusterId in state
    // const { clusterId } = this.state;
    const clusterId = 'abc-123';

    return (
      <DocumentTitle title="Try Graylog Enterprise">
        <div>
          <PageHeader title="Try Graylog Enterprise">
            {null}

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
                <BiggerFontSize>
                  <p>
                    Designed to meet the needs of resource-constrained IT Operations and Software Engineering teams,
                    Graylog Operations provides numerous productivity enhancements that will save you thousands of
                    hours per year in collecting and analyzing log data to uncover the root cause of performance,
                    outage, and error issues.
                  </p>
                  <ProductLink href="https://go2.graylog.org/request-graylog-operations" clusterId={clusterId}>
                    Request now
                  </ProductLink>
                </BiggerFontSize>
              </Col>
              <Col md={6}>
                <GraylogEnterpriseHeader>Graylog Security</GraylogEnterpriseHeader>
                <BiggerFontSize>
                  <p>
                    Extend Graylog Open’s capabilities for detecting, investigating, and responding to cybersecurity
                    threats with security-specific dashboards and alerts, anomaly detection AI/ML engine,
                    integrations with other security tools, SOAR capabilities, and numerous compliance reporting
                    features.
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
  },
});

export default EnterprisePage;
