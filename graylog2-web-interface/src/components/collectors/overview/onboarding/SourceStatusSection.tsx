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

import QuietSection from './QuietSection';

import { DividedIconRow, IconRowList } from '../../common/IconRowList';
import { OS_LABELS } from '../../common/Constants';

type Props = {
  instance: CollectorInstanceView;
  sources: Source[] | undefined;
  /** Whether the collector delivered any source messages in the preview window. */
  receiving: boolean;
};

const SourceName = styled.span`
  font-weight: 600;
`;

const RowStatus = styled.span<{ $variant?: 'muted' | 'success' | 'warning' }>(
  ({ $variant = 'muted', theme }) => css`
    margin-left: auto;
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

const SourceStatus = ({ source, instance, receiving }: { source: Source } & Omit<Props, 'sources'>) => {
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

  // The preview search counts messages per collector, not per source, so once anything arrives
  // every active source reads as receiving. Honest per-source rates need backend support.
  if (receiving) {
    return (
      <RowStatus $variant="success">
        <Icon name="check_circle" /> Receiving
      </RowStatus>
    );
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
const SourceStatusSection = ({ instance, sources, receiving }: Props) => {
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
          {sources.map((source) => (
            <DividedIconRow key={source.id}>
              <SourceName>{source.name}</SourceName>
              <SourceStatus source={source} instance={instance} receiving={receiving} />
            </DividedIconRow>
          ))}
        </IconRowList>
      )}
      {online && !receiving && <Footer>Checking every few seconds</Footer>}
    </QuietSection>
  );
};

export default SourceStatusSection;
