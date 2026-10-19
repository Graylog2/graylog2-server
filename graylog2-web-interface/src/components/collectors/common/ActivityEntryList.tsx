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
import { Trans, useTranslation } from 'react-i18next';

import { Icon, Link, RelativeTime } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import Routes from 'routing/Routes';
import { naturalSortIgnoreCase } from 'util/SortUtils';

import { DividedIconRow, IconRowList } from './IconRowList';

import type { ActivityEntry, TargetInfo } from '../types';

const DescriptionText = styled.span`
  flex: 1;
`;

const MutedText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
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

const TargetLink = ({ target }: { target: TargetInfo }) => {
  const { t } = useTranslation('collectors');

  if (!target.id) {
    return <MutedText>{t('activity.deleted_target')}</MutedText>;
  }

  if (target.type === 'fleet') {
    return <Link to={Routes.SYSTEM.COLLECTORS.FLEET(target.id)}>{target.name}</Link>;
  }

  return <Link to={Routes.SYSTEM.COLLECTORS.INSTANCES}>{target.name}</Link>;
};

const DESCRIPTION_KEYS: Record<ActivityEntry['type'], string> = {
  CONFIG_CHANGED: 'activity.config_changed',
  INGEST_CONFIG_CHANGED: 'activity.ingest_config_changed',
  RESTART: 'activity.restart',
  DISCOVERY_RUN: 'activity.discovery_run',
  FLEET_REASSIGNED: 'activity.reassigned',
};

const byName = (a: TargetInfo, b: TargetInfo) => naturalSortIgnoreCase(a.name, b.name);

const Description = ({
  entry,
  compareTargets,
}: {
  entry: ActivityEntry;
  compareTargets: (a: TargetInfo, b: TargetInfo) => number;
}) => {
  const { t } = useTranslation('collectors');
  const sortedTargets = entry.targets.toSorted(compareTargets);
  const target = sortedTargets[0];

  if (!target) {
    return <span>{entry.type}</span>;
  }

  const otherTargets = sortedTargets.length - 1;
  const destination = entry.type === 'FLEET_REASSIGNED' ? entry.details?.destination_fleet : undefined;
  // Fleet reassignments always target collectors.
  const targetType = entry.type === 'FLEET_REASSIGNED' ? 'collector' : target.type;
  const key = [
    DESCRIPTION_KEYS[entry.type],
    destination ? '_to_fleet' : '',
    `.${targetType}`,
    otherTargets > 0 ? '_with_others' : '',
  ].join('');

  return (
    <span>
      <Trans
        t={t}
        i18nKey={key}
        count={otherTargets > 0 ? otherTargets : undefined}
        components={{
          targetLink: <TargetLink target={target} />,
          destinationLink: destination ? <TargetLink target={destination} /> : <span />,
        }}
      />
    </span>
  );
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
const ActivityEntryList = ({ entries, compareTargets = byName }: Props) => {
  const { t } = useTranslation('collectors');

  return (
    <ActivityList>
      {entries.map((entry) => (
        <Row key={entry.seq}>
          <Icon name={ICON_MAP[entry.type] ?? 'info'} />
          <Content>
            <DescriptionText>
              <Description entry={entry} compareTargets={compareTargets} />
            </DescriptionText>
            <Meta>
              <MutedText>
                {entry.actor ? t('activity.by_actor', { name: entry.actor.full_name }) : t('activity.by_system')}
              </MutedText>
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
};

export default ActivityEntryList;
