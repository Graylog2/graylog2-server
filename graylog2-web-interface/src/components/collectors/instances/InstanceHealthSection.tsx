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

// Code-block chrome per this feature's convention (see InstallCommand's CommandBlock),
// except the wrapping: break-word keeps words intact and only splits tokens (URLs)
// that would otherwise overflow the narrow drawer.
const ErrorBlock = styled.pre(
  ({ theme }) => css`
    padding: ${theme.spacings.sm};
    background: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    white-space: pre-wrap;
    overflow-wrap: break-word;
    margin-top: ${theme.spacings.xs};
    margin-bottom: 0;
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
      <>
        {online ? (
          <Label bsStyle={healthy ? 'success' : 'danger'}>{stateText}</Label>
        ) : (
          // Stale (offline) health must not read as a live signal: the muted default
          // label + "Last known:" wording carry the de-emphasis (theme-safe, unlike
          // opacity dimming, which has no precedent in the product).
          <Label bsStyle="default">Last known: {stateText}</Label>
        )}
        {online && (
          <>
            {' '}
            {/* Only while online: offline time would silently inflate the duration
                (healthy for 1h, then offline 3 days is not "Healthy for 3 days");
                the Last Seen row above conveys staleness for offline instances. */}
            <Duration>
              for <RelativeTime dateTime={health.healthy_changed_at} withoutSuffix />
            </Duration>
          </>
        )}
        {/* Guard against malformed/stale reports carrying last_error alongside
            healthy: true — a green badge must not trail an unexplained error. */}
        {!healthy && lastError && <ErrorBlock data-testid="health-error">{lastError}</ErrorBlock>}
      </>
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
