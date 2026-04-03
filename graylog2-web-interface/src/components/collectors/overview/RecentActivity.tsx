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

import { Icon, Link, RelativeTime, Spinner, NoEntitiesExist } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import Routes from 'routing/Routes';

import { useRecentActivity } from '../hooks';
import type { ActivityEntry, TargetInfo } from '../types';

const SectionTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h3};
  `,
);

const SectionHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: ${theme.spacings.sm};
    margin-top: ${theme.spacings.lg};
  `,
);

const ActivityList = styled.ul`
  list-style: none;
  margin: 0;
  padding: 0;
`;

const ActivityRow = styled.li(
  ({ theme }) => css`
    display: flex;
    align-items: baseline;
    gap: ${theme.spacings.sm};
    padding: ${theme.spacings.xs} 0;
    border-bottom: 1px solid ${theme.colors.gray[90]};

    &:last-child {
      border-bottom: none;
    }
  `,
);

const Description = styled.span`
  flex: 1;
`;

const MutedText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    white-space: nowrap;
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
  if (target.type === 'fleet') {
    return <Link to={Routes.SYSTEM.COLLECTORS.FLEET(target.id)}>{target.name}</Link>;
  }

  return <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES}>{target.name}</Link>;
};

const renderDescription = (entry: ActivityEntry) => {
  const target = entry.targets[0];

  if (!target) {
    return <span>{entry.type}</span>;
  }

  switch (entry.type) {
    case 'CONFIG_CHANGED':
      return (
        <span>
          Configuration updated for {target.type} {targetLink(target)}
        </span>
      );
    case 'INGEST_CONFIG_CHANGED':
      return (
        <span>
          Ingest configuration updated for {target.type} {targetLink(target)}
        </span>
      );
    case 'RESTART':
      return (
        <span>
          Restart requested for {target.type} {targetLink(target)}
        </span>
      );
    case 'DISCOVERY_RUN':
      return (
        <span>
          Discovery run triggered for {target.type} {targetLink(target)}
        </span>
      );
    case 'FLEET_REASSIGNED': {
      const newFleetId = entry.details?.new_fleet_id;
      const newFleetName = entry.details?.new_fleet_name ?? newFleetId;

      return (
        <span>
          Collector {targetLink(target)} reassigned to fleet{' '}
          {newFleetId ? <Link to={Routes.SYSTEM.COLLECTORS.FLEET(newFleetId)}>{newFleetName}</Link> : 'unknown'}
        </span>
      );
    }
    default:
      return <span>{entry.type}</span>;
  }
};

const RecentActivity = () => {
  const { data, isLoading } = useRecentActivity();

  return (
    <div>
      <SectionHeader>
        <SectionTitle>Recent Activity</SectionTitle>
      </SectionHeader>

      {isLoading && <Spinner />}

      {!isLoading && (!data?.activities || data.activities.length === 0) && (
        <NoEntitiesExist>No recent activity.</NoEntitiesExist>
      )}

      {!isLoading && data?.activities && data.activities.length > 0 && (
        <ActivityList>
          {data.activities.map((entry) => (
            <ActivityRow key={entry.seq}>
              <Icon name={ICON_MAP[entry.type] ?? 'info'} />
              <Description>{renderDescription(entry)}</Description>
              <MutedText>{entry.actor ? `by ${entry.actor.full_name}` : 'by System'}</MutedText>
              {entry.timestamp && (
                <MutedText>
                  <RelativeTime dateTime={entry.timestamp} />
                </MutedText>
              )}
            </ActivityRow>
          ))}
        </ActivityList>
      )}
    </div>
  );
};

export default RecentActivity;
