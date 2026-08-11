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
import { useEffect, useRef, useState } from 'react';
import styled, { css } from 'styled-components';

import { Table } from 'components/bootstrap';
import { Link, RelativeTime } from 'components/common';
import InstanceStatusLabel from 'components/collectors/common/InstanceStatusLabel';
import MutedText from 'components/collectors/common/MutedText';
import { useInstances } from 'components/collectors/hooks/useInstanceQueries';
import PulsingDot from 'components/collectors/overview/onboarding/PulsingDot';
import type { PlatformId } from 'components/collectors/overview/onboarding/platforms';
import type { CollectorInstanceView } from 'components/collectors/types';
import Routes from 'routing/Routes';

const POLL_INTERVAL_MS = 3000;

type Props = {
  fleetId: string;
  fleetName: string;
  platformId: PlatformId | null;
};

const Container = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    padding: ${theme.spacings.md};
  `,
);

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: ${theme.spacings.sm};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const TitleGroup = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
  `,
);

const Title = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h4};
  `,
);

const Subtitle = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

/**
 * Live list of the hosts that enrolled since this component mounted. The parent remounts it per
 * generated token (via `key`), so the baseline snapshot always means "instances that existed
 * before this token could have been used". Diffing ids instead of comparing enrolled_at against
 * browser time avoids clock-skew bugs (same approach as WaitingForConnection).
 */
const EnrollingHostsList = ({ fleetId, fleetName, platformId }: Props) => {
  const { data: instances, error } = useInstances(fleetId, { refetchInterval: POLL_INTERVAL_MS, silent: true });
  const baseline = useRef<Set<string> | null>(null);
  const [enrolling, setEnrolling] = useState<CollectorInstanceView[]>([]);

  useEffect(() => {
    if (!instances) return;

    if (baseline.current === null) {
      baseline.current = new Set(instances.map((i) => i.id));

      return;
    }

    const known = baseline.current;
    const fresh = instances
      .filter((i) => !known.has(i.id))
      .sort((a, b) => (b.enrolled_at ?? '').localeCompare(a.enrolled_at ?? ''));

    setEnrolling(fresh);
  }, [instances]);

  const connectedCount = enrolling.filter((i) => i.status === 'online').length;

  return (
    <Container>
      <Header>
        <TitleGroup>
          <PulsingDot />
          <Title>Enrolling hosts</Title>
          <Subtitle>
            {enrolling.length > 0 ? `${connectedCount} connected · listening for more…` : 'listening…'}
          </Subtitle>
        </TitleGroup>
        <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES}>View all in Instances</Link>
      </Header>
      {enrolling.length > 0 ? (
        <Table condensed>
          <thead>
            <tr>
              <th>Host</th>
              <th>Status</th>
              <th>Enrolled</th>
            </tr>
          </thead>
          <tbody>
            {enrolling.map((i) => (
              <tr key={i.id}>
                <td>
                  <Link
                    to={Routes.SYSTEM.COLLECTORS.ONBOARDING_INSTANCE(i.instance_uid)}
                    state={{ platformId, fleetName }}>
                    {i.hostname ?? i.instance_uid}
                  </Link>
                </td>
                <td>
                  <InstanceStatusLabel status={i.status} />
                </td>
                <td>
                  <RelativeTime dateTime={i.enrolled_at} />
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      ) : (
        <MutedText>Hosts running the command appear here as they check in.</MutedText>
      )}
      {Boolean(error) && (
        <MutedText role="status">Having trouble reaching the server &mdash; retrying automatically</MutedText>
      )}
    </Container>
  );
};

export default EnrollingHostsList;
