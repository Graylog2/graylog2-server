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

import * as React from 'react';
import { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { reduce } from 'lodash';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import EventHandlersThrottler from 'util/EventHandlersThrottler';
import NumberUtils from 'util/NumberUtils';
import { useStore } from 'stores/connect';
import { Col, Row, Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { ClusterTrafficActions, ClusterTrafficStore } from 'stores/cluster/ClusterTrafficStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import { formatTrafficData } from 'util/TrafficUtils';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

import TrafficGraph from './TrafficGraph';

const DAYS = [
  30,
  90,
  180,
  365,
];

const Wrapper = styled.div`
  margin-bottom: 5px;

  .control-label {
    padding-top: 0;
  }

  .graph-days-select {
    display: flex;
    align-items: baseline;

    select {
      padding-top: 3px;
      height: 28px;
    }
  }
`;

const StyledDl = styled.dl`
  margin-bottom: 0;
`;
const StyledH2 = styled.h2(({ theme }: { theme: DefaultTheme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);
const StyledH3 = styled.h3(({ theme }: { theme: DefaultTheme }) => css`
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

const GraylogClusterTrafficGraph = () => {
  const { traffic } = useStore(ClusterTrafficStore);
  const [graphDays, setGraphDays] = useState(DAYS[0]);
  const [graphWidth, setGraphWidth] = useState(600);
  const eventThrottler = useRef(new EventHandlersThrottler());
  const containerRef = useRef(null);
  const licensePlugin = PluginStore.exports('license');
  const currentUser = useCurrentUser();

  const onGraphDaysChange = (event: React.ChangeEvent<HTMLOptionElement>): void => {
    event.preventDefault();
    const newDays = Number(event.target.value);

    setGraphDays(newDays);
  };

  useEffect(() => {
    ClusterTrafficActions.getTraffic(graphDays);
  }, [graphDays]);

  useEffect(() => {
    const _resizeGraphs = () => {
      const { clientWidth } = containerRef.current;

      setGraphWidth(clientWidth);
    };

    const _onResize = () => {
      eventThrottler.current.throttle(() => _resizeGraphs());
    };

    window.addEventListener('resize', _onResize);

    if (containerRef.current) {
      _resizeGraphs();
    }

    return () => {
      window.removeEventListener('resize', _onResize);
    };
  }, []);

  const TrafficGraphComponent = (isPermitted(currentUser.permissions, ['licenses:read']) && licensePlugin[0]?.EnterpriseTrafficGraph) || TrafficGraph;
  let sumOutput = null;
  let trafficGraph = <Spinner />;

  if (traffic) {
    const bytesOut = reduce(traffic.output, (result, value) => result + value);

    sumOutput = <small>Last {graphDays} days: {NumberUtils.formatBytes(bytesOut)}</small>;

    const unixTraffic = formatTrafficData(traffic.output);

    trafficGraph = (
      <TrafficGraphComponent traffic={unixTraffic}
                             width={graphWidth} />
    );
  }

  return (
    <>
      <Wrapper className="form-inline graph-days pull-right">
        <Input id="graph-days"
               type="select"
               bsSize="small"
               label="Days"
               value={graphDays}
               onChange={onGraphDaysChange}
               formGroupClassName="graph-days-select">
          {DAYS.map((size) => <option key={`option-${size}`} value={size}>{size}</option>)}
        </Input>
      </Wrapper>

      <StyledH3 ref={containerRef}>Outgoing traffic {sumOutput}</StyledH3>
      {trafficGraph}
    </>
  );
};

type Props = {
  layout?: 'default' | 'compact',
  children: React.ReactNode
}

const GraylogClusterOverview = ({ layout, children }: Props) => {
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
              <GraylogClusterTrafficGraph />
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
            <GraylogClusterTrafficGraph />
          </Col>
        </Row>
      </Col>
    </Row>
  );
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
