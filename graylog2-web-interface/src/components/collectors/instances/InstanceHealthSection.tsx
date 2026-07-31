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
import styled, { css } from 'styled-components';

import { Label } from 'components/bootstrap';
import { RelativeTime } from 'components/common';

import type { CollectorHealth } from '../types';

type Props = {
  health: CollectorHealth | null;
  online: boolean;
};

// Deliberately duplicated from InstanceDetailDrawer.tsx (module-private there); extraction is
// worth it at a third consumer, not two.
const Section = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const SectionTitle = styled.h4(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.body};
    font-weight: 600;
    border-bottom: 1px solid ${theme.colors.gray[80]};
    padding-bottom: ${theme.spacings.xs};
  `,
);

const EmptyText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

const Duration = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

// Stale (offline) health is shown but must not read as a live signal.
const Body = styled.div<{ $stale: boolean }>(
  ({ $stale }) => css`
    opacity: ${$stale ? 0.6 : 1};
  `,
);

const ErrorBlock = styled.pre(
  ({ theme }) => css`
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.variant.danger};
    margin-top: ${theme.spacings.xs};
    margin-bottom: 0;
    padding: ${theme.spacings.xs} ${theme.spacings.sm};
    /* Agent errors are often long single lines; wrap instead of forcing a
       horizontal scroller inside the narrow drawer. */
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  `,
);

const InstanceHealthSection = ({ health, online }: Props) => {
  let body: React.ReactNode;

  if (!health) {
    body = (
      <>
        <Label bsStyle="default">Unknown</Label>{' '}
        <EmptyText>This collector has not reported health information.</EmptyText>
      </>
    );
  } else {
    const { healthy, last_error: lastError } = health.component_health;
    const stateText = healthy ? 'Healthy' : 'Unhealthy';

    body = (
      <Body $stale={!online}>
        {online ? (
          <Label bsStyle={healthy ? 'success' : 'danger'}>{stateText}</Label>
        ) : (
          <Label bsStyle="default">Last known: {stateText}</Label>
        )}{' '}
        <Duration>
          for <RelativeTime dateTime={health.healthy_changed_at} withoutSuffix />
        </Duration>
        {lastError && <ErrorBlock data-testid="health-error">{lastError}</ErrorBlock>}
      </Body>
    );
  }

  return (
    <Section>
      <SectionTitle>Health</SectionTitle>
      {body}
    </Section>
  );
};

export default InstanceHealthSection;
