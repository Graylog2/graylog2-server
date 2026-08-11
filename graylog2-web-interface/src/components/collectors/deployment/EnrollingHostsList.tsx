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

import { Button, Table } from 'components/bootstrap';
import { Link, RelativeTime } from 'components/common';
import InstanceStatusLabel from 'components/collectors/common/InstanceStatusLabel';
import MutedText from 'components/collectors/common/MutedText';
import { useInstances } from 'components/collectors/hooks/useInstanceQueries';
import useFleetReceivingCounts from 'components/collectors/hooks/useFleetReceivingCounts';
import PulsingDot from 'components/collectors/overview/onboarding/PulsingDot';
import type { CollectorInstanceView } from 'components/collectors/types';
import Routes from 'routing/Routes';

import EnrollingHostSetup from './EnrollingHostSetup';

const POLL_INTERVAL_MS = 3000;

type Props = {
  fleetId: string;
  fleetName: string;
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

const Receiving = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.success};
  `,
);

const FirstMessagesCell = ({ count }: { count: number | undefined }) => {
  if ((count ?? 0) > 0) {
    return <Receiving>Receiving</Receiving>;
  }

  return <MutedText as="span">Listening&hellip;</MutedText>;
};

/**
 * Live list of the hosts that enrolled since this component mounted. The parent remounts it per
 * generated token (via `key`), so the baseline snapshot always means "instances that existed
 * before this token could have been used". Diffing ids instead of comparing enrolled_at against
 * browser time avoids clock-skew bugs (same approach as WaitingForConnection).
 */
const EnrollingHostsList = ({ fleetId, fleetName }: Props) => {
  const { data: instances, error } = useInstances(fleetId, { refetchInterval: POLL_INTERVAL_MS, silent: true });
  // One aggregation for all hosts, not a query per row.
  const { counts: receivingCounts } = useFleetReceivingCounts(fleetId);
  const baseline = useRef<Set<string> | null>(null);
  const [enrolling, setEnrolling] = useState<CollectorInstanceView[]>([]);
  // Expanding shows the concise connection-success view inline instead of navigating to the
  // detail page — leaving the wizard would lose the generated token and install command.
  const [expandedId, setExpandedId] = useState<string | null>(null);

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
              <th>First messages</th>
              <th>Enrolled</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {enrolling.map((i) => {
              const expanded = expandedId === i.id;

              return (
                <React.Fragment key={i.id}>
                  <tr>
                    <td>{i.hostname ?? i.instance_uid}</td>
                    <td>
                      <InstanceStatusLabel status={i.status} />
                    </td>
                    <td>
                      <FirstMessagesCell count={receivingCounts?.[i.instance_uid]} />
                    </td>
                    <td>
                      <RelativeTime dateTime={i.enrolled_at} />
                    </td>
                    <td>
                      <Button bsStyle="link" bsSize="xsmall" onClick={() => setExpandedId(expanded ? null : i.id)}>
                        {expanded ? 'Hide setup' : 'View setup'}
                      </Button>
                    </td>
                  </tr>
                  {expanded && (
                    <tr>
                      <td colSpan={5}>
                        <EnrollingHostSetup instance={i} fleetName={fleetName} />
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
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
