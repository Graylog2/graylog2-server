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
import reduce from 'lodash/reduce';
import styled, { css } from 'styled-components';
import { useMemo } from 'react';

import { Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { formatTrafficData } from 'util/TrafficUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { TrafficGraph, useGraphWidth } from 'components/common/Graph';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { Traffic } from 'components/common/Graph/types';
import { DAYS } from 'components/common/Graph/types';
import useGraphDays from 'components/common/Graph/contexts/useGraphDays';
import { getPrettifiedValue } from 'views/components/visualizations/utils/unitConverters';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';

const StyledH3 = styled.h3(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const Wrapper = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xs};

    .control-label {
      padding-top: 0;
    }

    .graph-days-select {
      display: flex;
      align-items: baseline;

      select {
        padding-top: ${theme.spacings.xxs};
      }
    }
  `,
);

type Props = {
  traffic: Traffic;
  trafficLimit?: number;
  title?: string;
};

const TrafficGraphWithDaySelect = ({ traffic, trafficLimit = undefined, title = undefined }: Props) => {
  const { graphDays, setGraphDays } = useGraphDays();
  const { graphWidth, graphContainerRef } = useGraphWidth();
  const { pathname } = useLocation();

  const sendTelemetry = useSendTelemetry();

  const onGraphDaysChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
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

  const bytesOut = useMemo(() => (traffic ? reduce(traffic, (result, value) => result + value) : null), [traffic]);
  const unixTraffic = useMemo(() => (traffic ? formatTrafficData(traffic) : null), [traffic]);

  const formattedTotalTraffic = useMemo(() => {
    const prettified = getPrettifiedValue(bytesOut, { abbrev: 'b', unitType: 'size' });

    return formatValueWithUnitLabel(prettified?.value, prettified.unit.abbrev);
  }, [bytesOut]);

  return (
    <>
      <Wrapper className="form-inline graph-days pull-right">
        <Input
          id="graph-days"
          type="select"
          bsSize="small"
          label="Days"
          value={graphDays}
          onChange={onGraphDaysChange}
          formGroupClassName="graph-days-select">
          {DAYS.map((size) => (
            <option key={`option-${size}`} value={size}>
              {size}
            </option>
          ))}
        </Input>
      </Wrapper>

      <StyledH3 ref={graphContainerRef}>
        {title ?? 'Outgoing traffic'}{' '}
        {bytesOut && (
          <small>
            Last {graphDays} days: {formattedTotalTraffic}
          </small>
        )}
      </StyledH3>
      {unixTraffic ? (
        <TrafficGraph trafficLimit={trafficLimit} traffic={unixTraffic} width={graphWidth} />
      ) : (
        <Spinner />
      )}
    </>
  );
};

export default TrafficGraphWithDaySelect;
