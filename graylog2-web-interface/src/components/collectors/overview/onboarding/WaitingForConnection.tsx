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
import { useState, useEffect, useRef } from 'react';
import styled, { css } from 'styled-components';

import { useInstances } from 'components/collectors/hooks/useInstanceQueries';
import type { CollectorInstanceView } from 'components/collectors/types';

import PulsingDot from './PulsingDot';

const POLL_INTERVAL_MS = 3000;

type Props = {
  fleetId: string | undefined;
  onConnected: (instance: CollectorInstanceView) => void;
};

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: ${theme.spacings.md};
    padding: ${theme.spacings.lg} 0;
  `,
);

const StatusRow = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
  `,
);

const StatusText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.body};
  `,
);

const WaitingForConnection = ({ fleetId, onConnected }: Props) => {
  const [elapsed, setElapsed] = useState(0);
  const { data: instances, error } = useInstances(fleetId, { refetchInterval: POLL_INTERVAL_MS, silent: true });
  // Instances that already existed when this step mounted. Anything beyond these is "ours".
  // Diffing ids instead of comparing enrolled_at against browser time avoids clock-skew bugs.
  const baseline = useRef<Set<string> | null>(null);
  const fired = useRef(false);

  useEffect(() => {
    const interval = setInterval(() => setElapsed((s) => s + 1), 1000);

    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!instances) return;

    const known = baseline.current;

    if (known === null) {
      baseline.current = new Set(instances.map((i) => i.id));

      return;
    }

    if (fired.current) return;

    const fresh = instances
      .filter((i) => !known.has(i.id))
      .sort((a, b) => (a.enrolled_at ?? '').localeCompare(b.enrolled_at ?? ''));

    if (fresh.length > 0) {
      fired.current = true;
      onConnected(fresh[0]);
    }
  }, [instances, onConnected]);

  return (
    <Container>
      <StatusRow>
        <PulsingDot />
        <StatusText>Waiting for connection... {elapsed}s</StatusText>
      </StatusRow>
      {Boolean(error) && (
        <StatusText role="status">Having trouble reaching the server &mdash; retrying automatically</StatusText>
      )}
    </Container>
  );
};

export default WaitingForConnection;
