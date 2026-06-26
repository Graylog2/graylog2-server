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

import { Icon, Link, RelativeTime } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import Routes from 'routing/Routes';
import { naturalSortIgnoreCase } from 'util/SortUtils';

import { DividedIconRow, IconRowList } from './IconRowList';

import type { ActivityEntry, TargetInfo } from '../types';

const Description = styled.span`
  flex: 1;
`;

const MutedText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    white-space: nowrap;
  `,
);

// Query container so the rows adapt to their own available width rather than the viewport: the same
// component renders inline in the wide overview feed and stacked in the narrow instance drawer.
const ActivityList = styled(IconRowList)`
  container-type: inline-size;
`;

// Below the xs container width (e.g. the narrow instance drawer) the actor/timestamp drop onto a
// line below the description so it no longer wraps early; in wider containers they stay inline.
const Row = styled(DividedIconRow)(
  ({ theme }) => css`
    @container (max-width: ${theme.breakpoints.max.xs}) {
      align-items: flex-start;
    }
  `,
);

const Content = styled.div(
  ({ theme }) => css`
    flex: 1;
    display: flex;
    align-items: baseline;
    gap: ${theme.spacings.sm};

    @container (max-width: ${theme.breakpoints.max.xs}) {
      flex-direction: column;
      align-items: flex-start;
      gap: ${theme.spacings.xxs};
    }
  `,
);

const Meta = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.xs};
  `,
);

const ICON_MAP: Record<ActivityEntry['type'], IconName> = {
  CONFIG_CHANGED: 'settings',
  INGEST_CONFIG_CHANGED: 'settings',
  RESTART: 'refresh',
  DISCOVERY_RUN: 'search',
  FLEET_REASSIGNED: 'shuffle',
};

const targetLink = (target: TargetInfo) => {
  if (!target.id) {
    return <MutedText>[deleted]</MutedText>;
  }

  if (target.type === 'fleet') {
    return <Link to={Routes.SYSTEM.COLLECTORS.FLEET(target.id)}>{target.name}</Link>;
  }

  return <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES}>{target.name}</Link>;
};

const additionalTargetText = (targets: TargetInfo[]) => {
  if (targets.length <= 1) {
    return null;
  }
  if (targets.length === 2) {
    return <span> and 1 other {targets[0].type}</span>;
  }

  return (
    <span>
      {' '}
      and {targets.length - 1} other {targets[0].type}s
    </span>
  );
};

const byName = (a: TargetInfo, b: TargetInfo) => naturalSortIgnoreCase(a.name, b.name);

const renderDescription = (entry: ActivityEntry, compareTargets: (a: TargetInfo, b: TargetInfo) => number) => {
  const sortedTargets = entry.targets.toSorted(compareTargets);
  const target = sortedTargets[0];

  if (!target) {
    return <span>{entry.type}</span>;
  }

  switch (entry.type) {
    case 'CONFIG_CHANGED':
      return (
        <span>
          Configuration updated for {target.type} {targetLink(target)}
          {additionalTargetText(sortedTargets)}
        </span>
      );
    case 'INGEST_CONFIG_CHANGED':
      return (
        <span>
          Ingest configuration updated for {target.type} {targetLink(target)}
          {additionalTargetText(sortedTargets)}
        </span>
      );
    case 'RESTART':
      return (
        <span>
          Restart requested for {target.type} {targetLink(target)}
          {additionalTargetText(sortedTargets)}
        </span>
      );
    case 'DISCOVERY_RUN':
      return (
        <span>
          Discovery run triggered for {target.type} {targetLink(target)}
          {additionalTargetText(sortedTargets)}
        </span>
      );
    case 'FLEET_REASSIGNED': {
      return (
        <span>
          Collector {targetLink(target)}
          {additionalTargetText(sortedTargets)} reassigned
          {entry.details && <> to fleet {targetLink(entry.details.destination_fleet)}</>}
        </span>
      );
    }
  }
};

type Props = {
  entries: ActivityEntry[];
  // How to order an entry's targets, deciding which one leads the description. Defaults to
  // alphabetical by name; the per-instance drawer overrides it to float the viewed collector first.
  compareTargets?: (a: TargetInfo, b: TargetInfo) => number;
};

/**
 * Renders transaction-log activity entries (icon, description, actor, time). Shared between the
 * overview's Recent Activity feed and the instance drawer's Pending Changes section, mirroring the
 * shared `ActivityEntryMapper` on the backend. The actor/timestamp stack below the description in
 * narrow containers (see {@link Content}).
 */
const ActivityEntryList = ({ entries, compareTargets = byName }: Props) => (
  <ActivityList>
    {entries.map((entry) => (
      <Row key={entry.seq}>
        <Icon name={ICON_MAP[entry.type] ?? 'info'} />
        <Content>
          <Description>{renderDescription(entry, compareTargets)}</Description>
          <Meta>
            <MutedText>{entry.actor ? `by ${entry.actor.full_name}` : 'by System'}</MutedText>
            {entry.timestamp && (
              <MutedText>
                <RelativeTime dateTime={entry.timestamp} />
              </MutedText>
            )}
          </Meta>
        </Content>
      </Row>
    ))}
  </ActivityList>
);

export default ActivityEntryList;
