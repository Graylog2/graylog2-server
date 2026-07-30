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

import { Icon, Link, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import type { CollectorInstanceView, Source, SourceType } from 'components/collectors/types';
import { formatNumber } from 'util/NumberFormatting';

import QuietSection from './QuietSection';

import { DividedIconRow, IconRowList } from '../../common/IconRowList';
import { OS_LABELS } from '../../common/Constants';

type Props = {
  instance: CollectorInstanceView;
  sources: Source[] | undefined;
  /** Whether the collector delivered any source messages in the preview window. */
  receiving: boolean;
  /** Message count per source id for the preview window. `undefined` when the aggregation failed. */
  sourceCounts?: Record<string, number>;
};

const SourceName = styled.span`
  font-weight: 600;
`;

const RowStatus = styled.span<{ $variant?: 'muted' | 'success' | 'warning' }>(
  ({ $variant = 'muted', theme }) => css`
    min-width: 12rem;
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.small};
    color: ${{
      muted: theme.colors.gray[60],
      success: theme.colors.variant.success,
      warning: theme.colors.variant.darker.warning,
    }[$variant]};
  `,
);

const SourceCount = styled.span(
  ({ theme }) => css`
    margin-left: auto;
    min-width: 6ch;
    text-align: right;
    font-size: ${theme.fonts.size.small};
    font-variant-numeric: tabular-nums;
    color: ${theme.colors.gray[60]};
  `,
);

const Footer = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.gray[60]};
  `,
);

// The platform a source type is restricted to; sources without an entry run everywhere.
const SOURCE_PLATFORM: Partial<Record<SourceType, string>> = {
  journald: 'linux',
  windows_event_log: 'windows',
};

const NO_COUNT = '—';

/** The count for a source, or an em dash when a number would be meaningless or unknown. */
const SourceCountCell = ({ count }: { count: number | undefined }) => (
  <SourceCount>{count === undefined ? NO_COUNT : formatNumber(count)}</SourceCount>
);

const SourceStatus = ({
  source,
  instance,
  receiving,
  count,
}: { source: Source; count: number | undefined } & Omit<Props, 'sources' | 'sourceCounts'>) => {
  const requiredPlatform = SOURCE_PLATFORM[source.type];

  if (!source.enabled) {
    return <RowStatus>Disabled</RowStatus>;
  }

  if (instance.os && requiredPlatform && requiredPlatform !== instance.os) {
    return <RowStatus>Not applicable on {OS_LABELS[instance.os] ?? instance.os}</RowStatus>;
  }

  if (instance.status !== 'online') {
    return <RowStatus $variant="warning">Paused &mdash; collector offline</RowStatus>;
  }

  if (count === undefined ? receiving : count > 0) {
    return (
      <RowStatus $variant="success">
        <Icon name="check_circle" /> Receiving
      </RowStatus>
    );
  }

  // Once the collector is proven to be delivering, a silent source is a steady state rather than a
  // pending one, so it gets no spinner.
  if (receiving) {
    return <RowStatus>No messages yet</RowStatus>;
  }

  return (
    <RowStatus>
      <Spinner text="Waiting for first messages..." delay={0} />
    </RowStatus>
  );
};

/**
 * The fleet's sources with a per-source status: what should be collecting on this host, what
 * cannot apply to its platform, and whether messages are flowing yet.
 */
const SourceStatusSection = ({ instance, sources, receiving, sourceCounts = undefined }: Props) => {
  const online = instance.status === 'online';

  return (
    <QuietSection
      title="Log sources"
      titleAs="h3"
      actions={<Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>Configure sources</Link>}>
      {!sources?.length ? (
        <Footer>No sources configured for this fleet yet.</Footer>
      ) : (
        <IconRowList>
          {sources.map((source) => {
            // A present map with no entry for this source means it produced nothing; an absent map
            // means the aggregation is unavailable and no number should be claimed.
            const count = sourceCounts ? (sourceCounts[source.id] ?? 0) : undefined;
            // Mirrors status rules 1–2 only. A source restricted to a *matching* platform is still
            // collecting, so testing `SOURCE_PLATFORM[source.type]` alone would be too strict.
            const requiredPlatform = SOURCE_PLATFORM[source.type];
            const mismatched = Boolean(instance.os && requiredPlatform && requiredPlatform !== instance.os);
            const collecting = source.enabled && !mismatched;

            return (
              <DividedIconRow key={source.id}>
                <SourceName>{source.name}</SourceName>
                <SourceCountCell count={collecting ? count : undefined} />
                <SourceStatus source={source} instance={instance} receiving={receiving} count={count} />
              </DividedIconRow>
            );
          })}
        </IconRowList>
      )}
      {online && (
        <Footer>Messages received in the last 15 minutes{!receiving && ' · checking every few seconds'}</Footer>
      )}
    </QuietSection>
  );
};

export default SourceStatusSection;
