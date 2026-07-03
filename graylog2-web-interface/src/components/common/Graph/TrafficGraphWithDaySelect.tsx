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

import { Button } from 'components/bootstrap';
import { Spinner } from 'components/common';
import Select from 'components/common/Select';
import { formatTrafficData } from 'util/TrafficUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { TrafficGraph, useGraphWidth, useTrafficGraphZoom } from 'components/common/Graph';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { Traffic, TrafficType } from 'components/common/Graph/types';
import { DAYS, TRAFFIC_TYPE_APP_SECTIONS, TRAFFIC_TYPE_LABELS } from 'components/common/Graph/types';
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
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    margin-bottom: ${theme.spacings.xs};
  `,
);

const SelectGroup = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
  `,
);

const SelectLabel = styled.label`
  margin: 0;
`;

const TRAFFIC_TYPE_OPTIONS = Object.entries(TRAFFIC_TYPE_LABELS).map(([value, label]) => ({ value, label }));
const DAY_OPTIONS = DAYS.map((days) => ({ value: days, label: String(days) }));

type Props = {
  traffic?: Traffic;
  trafficLimit?: number;
  title?: string;
  trafficType?: TrafficType;
  onTrafficTypeChange?: (trafficType: TrafficType) => void;
};

const TrafficGraphWithDaySelect = ({
  traffic = undefined,
  trafficLimit = undefined,
  title = undefined,
  trafficType = 'output',
  onTrafficTypeChange = undefined,
}: Props) => {
  const { graphDays, setGraphDays } = useGraphDays();
  const { graphWidth, graphContainerRef } = useGraphWidth();

  const bytesOut = useMemo(() => (traffic ? reduce(traffic, (result, value) => result + value) : null), [traffic]);
  const unixTraffic = useMemo(() => (traffic ? formatTrafficData(traffic) : null), [traffic]);

  // The zoom gate compares the PLOTTED series (daily sums) against the limit, not the raw buckets.
  const { zoomedToData, uiRevision, canZoomOrReset, onZoomReset, onUserZoom, onUserZoomReset } = useTrafficGraphZoom(
    unixTraffic,
    trafficLimit,
  );
  const { pathname } = useLocation();

  const sendTelemetry = useSendTelemetry();

  const onGraphDaysChange = (newDays: number) => {
    // react-select fires onChange even when the already-selected option is picked again.
    if (newDays === graphDays) {
      return;
    }

    setGraphDays(newDays);

    sendTelemetry(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_DAYS_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: TRAFFIC_TYPE_APP_SECTIONS[trafficType],
      app_action_value: 'trafficgraph-days-button',
      event_details: { value: newDays },
    });
  };

  const onTrafficTypeSelect = (newTrafficType: TrafficType) => {
    if (newTrafficType === trafficType) {
      return;
    }

    onTrafficTypeChange(newTrafficType);

    sendTelemetry(TELEMETRY_EVENT_TYPE.TRAFFIC_GRAPH_TYPE_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: TRAFFIC_TYPE_APP_SECTIONS[newTrafficType],
      app_action_value: 'trafficgraph-type-select',
      event_details: { value: newTrafficType },
    });
  };

  const formattedTotalTraffic = useMemo(() => {
    const prettified = getPrettifiedValue(bytesOut, { abbrev: 'b', unitType: 'binary_size' });

    return formatValueWithUnitLabel(prettified?.value, prettified.unit.abbrev);
  }, [bytesOut]);

  return (
    <>
      <Wrapper className="form-inline graph-days pull-right">
        {onTrafficTypeChange && (
          <SelectGroup>
            <SelectLabel htmlFor="traffic-type">Show</SelectLabel>
            <Select
              inputId="traffic-type"
              aria-label="Show traffic type"
              size="small"
              compact
              clearable={false}
              searchable={false}
              options={TRAFFIC_TYPE_OPTIONS}
              value={trafficType}
              onChange={onTrafficTypeSelect}
            />
          </SelectGroup>
        )}
        <SelectGroup>
          <SelectLabel htmlFor="graph-days">Days</SelectLabel>
          <Select
            inputId="graph-days"
            aria-label="Days"
            size="small"
            compact
            clearable={false}
            searchable={false}
            options={DAY_OPTIONS}
            value={graphDays}
            onChange={onGraphDaysChange}
          />
        </SelectGroup>
        <Button type="button" bsSize="small" onClick={onZoomReset} disabled={!canZoomOrReset}>
          Zoom/Reset
        </Button>
      </Wrapper>

      <StyledH3 ref={graphContainerRef}>
        {title ?? TRAFFIC_TYPE_LABELS[trafficType]}{' '}
        {bytesOut != null && (
          <small>
            Last {graphDays} days: {formattedTotalTraffic}
          </small>
        )}
      </StyledH3>
      {unixTraffic ? (
        <TrafficGraph
          trafficLimit={trafficLimit}
          traffic={unixTraffic}
          width={graphWidth}
          zoomedToData={zoomedToData}
          uiRevision={uiRevision}
          onUserZoom={onUserZoom}
          onUserZoomReset={onUserZoomReset}
        />
      ) : (
        <Spinner />
      )}
    </>
  );
};

export default TrafficGraphWithDaySelect;
