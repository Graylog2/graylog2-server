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
import { useMemo } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { EmptyEntity, Link } from 'components/common';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';

import FleetCard, { getHealthStatus } from './FleetCard';

import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { FleetStatsSummary } from '../types';

const Grid = styled.div(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: ${theme.spacings.sm};
  `,
);

const EmptyState = styled.div(
  ({ theme }) => css`
    text-align: center;
    padding: ${theme.spacings.xxl};
    color: ${theme.colors.gray[60]};
  `,
);

type Props = {
  fleets: FleetStatsSummary[];
  filter: string;
};

const FleetCardsGrid = ({ fleets, filter }: Props) => {
  const history = useHistory();
  const sendTelemetry = useSendCollectorsTelemetry();

  const filtered = useMemo(() => {
    if (!filter) return fleets;
    const lower = filter.toLowerCase();

    return fleets.filter((f) => f.fleet_name.toLowerCase().includes(lower));
  }, [fleets, filter]);

  if (fleets.length === 0) {
    return (
      <EmptyEntity title="No fleets yet">
        <p>
          Fleets let you manage groups of Collectors with shared configuration. Create your first fleet, then add
          sources to define what data its Collectors should collect. Once configured,{' '}
          <Link to={Routes.SYSTEM.COLLECTORS.DEPLOYMENT}>deploy Collectors</Link> using an enrollment token.
        </p>
        <Button bsStyle="success" onClick={() => history.push(Routes.SYSTEM.COLLECTORS.FLEETS)}>
          Create Fleet
        </Button>
      </EmptyEntity>
    );
  }

  if (filtered.length === 0) {
    return <EmptyState>No fleets matching filter</EmptyState>;
  }

  return (
    <Grid>
      {filtered.map((fleet) => (
        <FleetCard
          key={fleet.fleet_id}
          stats={fleet}
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.OVERVIEW.FLEET_CARD_CLICKED, {
              app_action_value: 'fleet-card',
              fleet_id: fleet.fleet_id,
              health: getHealthStatus(fleet),
              online_instances: fleet.online_instances,
              offline_instances: fleet.offline_instances,
            });
            history.push(Routes.SYSTEM.COLLECTORS.FLEET(fleet.fleet_id));
          }}
        />
      ))}
    </Grid>
  );
};

export default FleetCardsGrid;
