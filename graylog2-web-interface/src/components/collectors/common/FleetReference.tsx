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

import { Link } from 'components/common';
import Routes from 'routing/Routes';

import { useFleets } from '../hooks';

const Hint = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
  `,
);

type Props = {
  fleetId: string;
  // The fleet the instance is being moved to, until the collector checks in and picks it up.
  pendingFleetId?: string | null;
  onClick?: () => void;
};

/**
 * Renders the fleet an instance belongs to: a link to the fleet, the target of a pending reassignment, or a
 * placeholder when the fleet no longer exists.
 *
 * Seeing an instance requires read access to its current fleet, so a current fleet missing from the fleet list
 * has been deleted. A pending target may be a fleet the user cannot read, so it is never labelled as removed.
 */
const FleetReference = ({ fleetId, pendingFleetId = null, onClick = undefined }: Props) => {
  const { data: fleets } = useFleets();
  const fleetNames = useMemo(() => new Map((fleets ?? []).map((fleet) => [fleet.id, fleet.name])), [fleets]);
  // Until the fleets have loaded, assume a fleet exists instead of flashing a "removed" placeholder.
  const isKnown = (id: string) => fleets === undefined || fleetNames.has(id);

  const fleetLink = (id: string) => (
    <Link to={Routes.SYSTEM.COLLECTORS.FLEET(id)} onClick={onClick}>
      {fleetNames.get(id) ?? id}
    </Link>
  );

  if (pendingFleetId) {
    const origin = isKnown(fleetId) ? (fleetNames.get(fleetId) ?? fleetId) : 'a removed fleet';

    return (
      <span>
        {isKnown(pendingFleetId) ? fleetLink(pendingFleetId) : pendingFleetId}{' '}
        <Hint title="The collector moves to this fleet the next time it checks in.">(moving from {origin})</Hint>
      </span>
    );
  }

  if (!isKnown(fleetId)) {
    return <Hint title="This fleet has been deleted.">Fleet removed ({fleetId})</Hint>;
  }

  return fleetLink(fleetId);
};

export default FleetReference;
