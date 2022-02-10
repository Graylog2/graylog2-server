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
/* eslint-disable react/no-find-dom-node */
import NumberUtils from 'util/NumberUtils';
import EventHandlersThrottler from 'util/EventHandlersThrottler';

import * as React from 'react';
import { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { useStore } from 'stores/connect';
import { Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { ClusterTrafficActions, ClusterTrafficStore } from 'stores/cluster/ClusterTrafficStore';
import { NodesStore } from 'stores/nodes/NodesStore';

import TrafficGraph from './TrafficGraph';

type Props = {
  layout: string,
  children: React.ReactNode
}

const StyledDl = styled.dl`
  margin-bottom: 0;
`;
const StyledH2 = styled.h2(({ theme }: { theme: DefaultTheme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);
const StyledH3 = styled.h3(({ theme }: { theme: DefaultTheme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

const GraylogClusterOverview = ({ layout, children }: Props) => {
  const nodes = useStore(NodesStore);
  const { traffic } = useStore(ClusterTrafficStore);
  const [graphWidth, setGraphWidth] = useState(600);
  const eventThrottler = useRef(new EventHandlersThrottler());
  const containerRef = useRef(null);
  const licensePlugin = PluginStore.exports('license');

  useEffect(() => {
    const _resizeGraphs = () => {
      const { clientWidth } = containerRef.current;

      setGraphWidth(clientWidth);
    };

    const _onResize = () => {
      eventThrottler.current.throttle(() => _resizeGraphs());
    };

    ClusterTrafficActions.getTraffic();
    window.addEventListener('resize', _onResize);

    if (containerRef) {
      _resizeGraphs();
    }

    return () => {
      window.removeEventListener('resize', _onResize);
    };
  }, [containerRef]);

  const renderClusterInfo = () => {
    let content = <Spinner />;

    if (nodes) {
      const { clusterId, nodeCount } = nodes;

      content = (
        <StyledDl className="system-dl">
          <dt>Cluster ID:</dt>
          <dd>{clusterId || 'Not available'}</dd>
          <dt>Number of nodes:</dt>
          <dd>{nodeCount}</dd>
        </StyledDl>
      );
    }

    return content;
  };

  const renderTrafficGraph = () => {
    const TrafficGraphComponent = licensePlugin[0]?.EnterpriseTrafficGraph || TrafficGraph;
    let sumOutput = null;
    let trafficGraph = <Spinner />;

    if (traffic) {
      const bytesOut = _.reduce(traffic.output, (result, value) => result + value);

      sumOutput = <small>Last 30 days: {NumberUtils.formatBytes(bytesOut)}</small>;

      trafficGraph = (
        <TrafficGraphComponent traffic={traffic.output}
                               from={traffic.from}
                               to={traffic.to}
                               width={graphWidth} />
      );
    }

    return (
      <>
        <StyledH3 ref={containerRef}>Outgoing traffic {sumOutput}</StyledH3>
        {trafficGraph}
      </>
    );
  };

  const renderHeader = () => {
    return <StyledH2>Graylog cluster</StyledH2>;
  };

  const renderDefaultLayout = () => {
    return (
      <Row className="content">
        <Col md={12}>
          {renderHeader()}
          {renderClusterInfo()}
          <hr />
          {children}
          <Row>
            <Col md={12}>
              {renderTrafficGraph()}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  };

  const renderCompactLayout = () => {
    return (
      <Row className="content">
        <Col md={12}>
          {renderHeader()}
          <Row>
            <Col md={6}>
              {renderClusterInfo()}
              <hr />
              {children}
            </Col>
            <Col md={6}>
              {renderTrafficGraph()}
            </Col>
          </Row>
        </Col>
      </Row>
    );
  };

  if (layout === 'compact') {
    return renderCompactLayout();
  }

  return renderDefaultLayout();
};

GraylogClusterOverview.propTypes = {
  layout: PropTypes.oneOf(['default', 'compact']),
  children: PropTypes.node,
};

GraylogClusterOverview.defaultProps = {
  layout: 'default',
  children: null,
};

export default GraylogClusterOverview;
