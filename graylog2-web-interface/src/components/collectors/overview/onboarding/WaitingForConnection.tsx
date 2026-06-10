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
import { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';

import PulsingDot from './PulsingDot';

type Props = {
  onSimulateConnection: () => void;
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

const WaitingForConnection = ({ onSimulateConnection }: Props) => {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => setElapsed((s) => s + 1), 1000);

    return () => clearInterval(interval);
  }, []);

  return (
    <Container>
      <StatusRow>
        <PulsingDot />
        <StatusText>Waiting for connection... {elapsed}s</StatusText>
      </StatusRow>
      <Button bsStyle="primary" bsSize="sm" onClick={onSimulateConnection}>
        Simulate connection
      </Button>
    </Container>
  );
};

export default WaitingForConnection;
