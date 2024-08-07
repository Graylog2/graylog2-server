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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { useStore } from 'stores/connect';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import { Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { NodesStore } from 'stores/nodes/NodesStore';
import ClusterTrafficGraph from 'components/cluster/ClusterTrafficGraph';

const StyledDl = styled.dl`
  margin-bottom: 0;
`;
const StyledH2 = styled.h2(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

const Header = () => <StyledH2>Graylog cluster</StyledH2>;

const ClusterInfo = () => {
  const nodes = useStore(NodesStore);

  if (!nodes) {
    return <Spinner />;
  }

  const { clusterId, nodeCount } = nodes;

  return (
    <StyledDl className="system-dl">
      <dt>Cluster ID:</dt>
      <dd>{clusterId || 'Not available'}</dd>
      <dt>Number of nodes:</dt>
      <dd>{nodeCount}</dd>
    </StyledDl>
  );
};

type Props = {
  layout?: 'default' | 'compact',
  children: React.ReactNode,
  showLicenseGraph?: boolean,
}

const GraylogClusterOverview = ({ layout, children, showLicenseGraph }: Props) => {
  const licensePlugin = PluginStore.exports('license');
  const currentUser = useCurrentUser();

  const LicenseGraphComponent = (isPermitted(currentUser.permissions, ['licenses:read']) && licensePlugin[0]?.LicenseGraphWithMetrics) || ClusterTrafficGraph;
  const EnterpriseGraphComponent = (isPermitted(currentUser.permissions, ['licenses:read']) && licensePlugin[0]?.EnterpriseTrafficGraph) || ClusterTrafficGraph;

  if (layout === 'compact') {
    return (
      <Row className="content">
        <Col md={12}>
          <Header />
          <Row>
            <Col md={6}>
              <ClusterInfo />
              <hr />
              {children}
            </Col>
            <Col md={6}>
              {showLicenseGraph ? (<LicenseGraphComponent />
              ) : (<EnterpriseGraphComponent />)}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }

  return (
    <Row className="content">
      <Col md={12}>
        <Header />
        <ClusterInfo />
        <hr />
        {children}
        <Row>
          <Col md={12}>
            {showLicenseGraph ? (<LicenseGraphComponent />
            ) : (<EnterpriseGraphComponent />)}
          </Col>
        </Row>
      </Col>
    </Row>
  );
};

GraylogClusterOverview.propTypes = {
  layout: PropTypes.oneOf(['default', 'compact']),
  children: PropTypes.node,
  showLicenseGraph: PropTypes.bool,
};

GraylogClusterOverview.defaultProps = {
  layout: 'default',
  children: null,
  showLicenseGraph: false,
};

export default GraylogClusterOverview;
