import * as React from 'react';
import PropTypes from 'prop-types';
import { useState, useEffect } from 'react';
import reduce from 'lodash/reduce';
import styled, { css } from 'styled-components';

import NumberUtils from 'util/NumberUtils';
import { Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import type { Traffic } from 'components/cluster/types';
import { formatTrafficData } from 'util/TrafficUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { TrafficGraph, useGraphWidth } from 'components/common/Graph';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

const DAYS = [
  30,
  90,
  180,
  365,
];

const StyledH3 = styled.h3(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

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

type Props = {
  getTraffic: (days: number) => void,
  traffic: Traffic,
  trafficLimit?: number,
  title?: string,
};

const TrafficGraphWithDaySelect = ({ getTraffic, traffic, trafficLimit, title } : Props) => {
  const [graphDays, setGraphDays] = useState(DAYS[0]);
  const { graphWidth, graphContainerRef } = useGraphWidth();
  const { pathname } = useLocation();

  const sendTelemetry = useSendTelemetry();

  const onGraphDaysChange = (event: React.ChangeEvent<HTMLOptionElement>): void => {
    event.preventDefault();
    const newDays = Number(event.target.value);

    setGraphDays(newDays);

    sendTelemetry(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_DAYS_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'outgoing-traffic',
      app_action_value: 'trafficgraph-days-button',
      event_details: { value: newDays },
    });
  };

  useEffect(() => {
    getTraffic(graphDays);
  }, [getTraffic, graphDays]);

  let sumOutput = null;
  let trafficGraph = <Spinner />;

  if (traffic) {
    const bytesOut = reduce(traffic.output, (result, value) => result + value);

    sumOutput = <small>Last {graphDays} days: {NumberUtils.formatBytes(bytesOut)}</small>;

    const unixTraffic = formatTrafficData(traffic.output);

    trafficGraph = (
      <TrafficGraph traffic={unixTraffic}
                    trafficLimit={trafficLimit}
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

      <StyledH3 ref={graphContainerRef}>{title ?? 'Outgoing traffic'} {sumOutput}</StyledH3>
      {trafficGraph}
    </>
  );
};

TrafficGraphWithDaySelect.propTypes = {
  traffic: PropTypes.object.isRequired, // traffic is: {"2017-11-15T15:00:00.000Z": 68287229, ...}
  trafficLimit: PropTypes.number,
  getTraffic: PropTypes.func.isRequired,
  title: PropTypes.string,
};

TrafficGraphWithDaySelect.defaultProps = {
  trafficLimit: undefined,
  title: undefined,
};

export default TrafficGraphWithDaySelect;
