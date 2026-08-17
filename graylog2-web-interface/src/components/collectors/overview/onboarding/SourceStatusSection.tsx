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

import { Icon, LinkContainer, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import type { CollectorInstanceView, Source, SourceType } from 'components/collectors/types';
import { formatNumber } from 'util/NumberFormatting';
import { Button } from 'components/bootstrap';

import QuietSection from './QuietSection';

import { OS_LABELS } from '../../common/Constants';
import { SOURCE_TYPE_LABELS } from '../../sources/Constants';

type Props = {
  instance: CollectorInstanceView;
  sources: Source[] | undefined;
  /** Whether the collector delivered any source messages in the preview window. */
  receiving: boolean;
  /** Message count per source id for the preview window. `undefined` when the aggregation failed. */
  sourceCounts?: Record<string, number>;
};

/* The rows are a three-part flex layout, not a table: the name takes the flexible space, the
   count right-aligns against the status, and the status occupies a fixed-width end column so the
   rows line up. Auto-sized table columns cannot express the count/status alignment. */
const SourceList = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

const SourceRow = styled.li(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    padding: ${theme.spacings.xs} 0;
    border-bottom: 1px solid ${theme.colors.gray[90]};

    &:last-child {
      border-bottom: none;
    }
  `,
);

const SourceName = styled.span`
  font-weight: 600;

  /* A long source name is otherwise the only row member with no size floor, so nothing shrinks
     and the row overflows the section; a flex child also defaults to min-width auto and would
     refuse to shrink below its content's width without this. */
  flex: 1;
  min-width: 50px;
  overflow-wrap: break-word;
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
    color: ${theme.colors.text.secondary};
  `,
);

const Footer = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
  `,
);

// The platform a source type is restricted to; sources without an entry run everywhere.
const SOURCE_PLATFORM: Partial<Record<SourceType, string>> = {
  journald: 'linux',
  windows_event_log: 'windows',
};

/**
 * Whether the frontend knows this source type at all.
 *
 * Types can exist in the database before the frontend learns about them — a source type whose
 * definition is still on an unmerged branch is stored and returned by the API while `SourceType` and
 * `SOURCE_TYPE_LABELS` have no entry for it. `SOURCE_TYPE_LABELS` is exhaustive over `SourceType`, so
 * membership in it is the registry of known types; `SOURCE_PLATFORM` is not, because types that run
 * everywhere are legitimately absent from it.
 *
 * This is a runtime check against a case the `SourceType` union claims cannot happen, which is why it
 * tests the object rather than comparing to a list of literals.
 */
const isRecognised = (type: SourceType) => Object.hasOwn(SOURCE_TYPE_LABELS, type);

const NO_COUNT = '—';

const countLabel = (count: number | undefined) => {
  if (count === undefined) {
    return NO_COUNT;
  }

  // A zero would sit right next to "No messages yet" or "Waiting for first messages...", which
  // already say it; the digit only adds noise.
  if (count === 0) {
    return '';
  }

  return `${formatNumber(count)} messages`;
};

/** The count for a source, or an em dash when a number would be meaningless or unknown. */
const SourceCountCell = ({ count }: { count: number | undefined }) => <SourceCount>{countLabel(count)}</SourceCount>;

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

  // Deliberately not "Not applicable": that asserts a definite incompatibility, whereas an
  // unrecognised type may well be collecting perfectly well server-side. We only decline to guess.
  if (!isRecognised(source.type)) {
    return <RowStatus>Unrecognised source type</RowStatus>;
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
      actions={
        <LinkContainer to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>
          <Button bsSize="xsmall">Configure sources</Button>
        </LinkContainer>
      }>
      {!sources?.length ? (
        <Footer>No sources configured for this fleet yet.</Footer>
      ) : (
        <SourceList>
          {sources.map((source) => {
            // A present map with no entry for this source means it produced nothing; an absent map
            // means the aggregation is unavailable and no number should be claimed.
            const count = sourceCounts ? (sourceCounts[source.id] ?? 0) : undefined;
            // Mirrors the structural status rules only. A source restricted to a *matching* platform
            // is still collecting, so testing `SOURCE_PLATFORM[source.type]` alone would be too
            // strict. An unrecognised type claims no number either: we cannot say whether a zero
            // means "silent" or "we do not understand this source".
            const requiredPlatform = SOURCE_PLATFORM[source.type];
            const mismatched = Boolean(instance.os && requiredPlatform && requiredPlatform !== instance.os);
            const collecting = source.enabled && !mismatched && isRecognised(source.type);

            return (
              <SourceRow key={source.id}>
                <SourceName>{source.name}</SourceName>
                <SourceCountCell count={collecting ? count : undefined} />
                <SourceStatus source={source} instance={instance} receiving={receiving} count={count} />
              </SourceRow>
            );
          })}
        </SourceList>
      )}
      {online && (
        <Footer>Messages received in the last 15 minutes{!receiving && ' · checking every few seconds'}</Footer>
      )}
    </QuietSection>
  );
};

export default SourceStatusSection;
