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
import { useEffect, useMemo, useState } from 'react';

import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { useStore } from 'stores/connect';
import LoggingSubsystem from 'components/loggers/LoggingSubsystem';
import LogLevelMetricsOverview from 'components/loggers/LogLevelMetricsOverview';
import { Col, Row, Button } from 'components/bootstrap';
import { LinkToNode, IfPermitted, Icon } from 'components/common';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props = {
  nodeId: string,
  subsystems: {},
}
const metric_name = 'org.apache.logging.log4j.core.Appender.all';

const NodeLoggers = ({ nodeId, subsystems }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();

  const { metrics } = useStore(MetricsStore);
  const [showDetails, setShowDetails] = useState(false);

  useEffect(() => {
    MetricsActions.add(nodeId, metric_name);

    return () => { MetricsActions.remove(nodeId, metric_name); };
  }, [nodeId]);

  const _formattedThroughput = useMemo(() => {
    if (metrics?.[nodeId]?.[metric_name]) {
      const { metric } = metrics[nodeId][metric_name];

      return 'rate' in metric ? metric.rate.total : 'n/a';
    }

    return 'n/a';
  }, [metrics, nodeId]);

  const subsystemKeys = Object.keys(subsystems)
    .map((subsystem) => (
      <LoggingSubsystem name={subsystem}
                        nodeId={nodeId}
                        key={`logging-subsystem-${nodeId}-${subsystem}`}
                        subsystem={subsystems[subsystem]} />
    ));

  const logLevelMetrics = <LogLevelMetricsOverview nodeId={nodeId} />;

  return (
    <Row className="row-sm log-writing-node content">
      <Col md={12}>
        <IfPermitted permissions="loggers:read">
          <div style={{ marginBottom: '20' }}>
            <div className="pull-right">
              <Button bsSize="sm"
                      bsStyle="primary"
                      className="trigger-log-level-metrics"
                      onClick={() => {
                        setShowDetails((prevShowDetails) => !prevShowDetails);

                        sendTelemetry(TELEMETRY_EVENT_TYPE.LOGGING.SHOW_LOG_LEVEL_METRICS_TOGGLED, {
                          app_pathname: getPathnameWithoutId(location.pathname),
                          app_section: 'log-level',
                          app_action_value: 'show-metrics',
                          event_details: { showing: !showDetails },
                        });
                      }}>
                <Icon name="speed" />{' '}
                {showDetails ? 'Hide' : 'Show'} log level metrics
              </Button>
            </div>
            <h2>
              <LinkToNode nodeId={nodeId} />{' '}
              <small>
                Has written a total of <strong>{_formattedThroughput} internal log messages.</strong>
              </small>
            </h2>
          </div>
          <div className="subsystems">
            {subsystemKeys}
          </div>
          {showDetails && logLevelMetrics}
        </IfPermitted>
      </Col>
    </Row>
  );
};

export default NodeLoggers;
