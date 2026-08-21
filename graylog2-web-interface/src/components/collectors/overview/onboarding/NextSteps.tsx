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

import useProductName from 'brand-customization/useProductName';
import { Icon, Link } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import Routes from 'routing/Routes';
import type { CollectorInstanceView } from 'components/collectors/types';

import { IconRow, IconRowList } from '../../common/IconRowList';
import collectorReceivedMessagesUrl from '../../common/collectorReceivedMessagesUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from '../../common/fields';

type Props = {
  instance: CollectorInstanceView;
};

const PanelTitle = styled.h4(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    font-weight: 600;
  `,
);

const LinkDescription = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
  `,
);

const TroubleshootingList = styled.ol(
  ({ theme }) => css`
    margin: 0;
    padding-left: ${theme.spacings.lg};
    font-size: ${theme.fonts.size.small};

    > li {
      margin-bottom: ${theme.spacings.xs};
    }
  `,
);

type NextLink = { icon: IconName; title: string; description: string; to: string };

const nextLinks = (instance: CollectorInstanceView): Array<NextLink> => [
  {
    icon: 'search',
    title: 'Explore your data',
    description: 'Open search filtered to this collector',
    to: collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid),
  },
  {
    icon: 'settings',
    title: 'Configure sources',
    description: 'Add or tune what this fleet collects',
    to: Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id),
  },
  {
    icon: 'hub',
    title: 'Manage fleets',
    description: 'Move this collector to a permanent fleet',
    to: Routes.SYSTEM.COLLECTORS.FLEETS,
  },
  {
    icon: 'add_circle',
    title: 'Install another collector',
    description: 'Repeat this setup on another host',
    to: Routes.SYSTEM.COLLECTORS.OVERVIEW,
  },
];

/**
 * The panel below the onboarding timeline: pointers to what to do next while everything is
 * healthy, or recovery steps when the collector has gone offline.
 */
const NextSteps = ({ instance }: Props) => {
  const productName = useProductName();
  if (instance.status !== 'online') {
    return (
      <div>
        <PanelTitle>Get It Back Online</PanelTitle>
        <TroubleshootingList>
          <li>Check that the collector service is running on the host.</li>
          <li>Verify the host can reach this {productName} server.</li>
          <li>Review the collector&apos;s own logs in the preview below for connection errors.</li>
        </TroubleshootingList>
      </div>
    );
  }

  return (
    <div>
      <PanelTitle>What&apos;s Next</PanelTitle>
      <IconRowList>
        {nextLinks(instance).map(({ icon, title, description, to }) => (
          <IconRow key={icon}>
            <Icon name={icon} />
            <div>
              <Link to={to}>{title}</Link>
              <LinkDescription>{description}</LinkDescription>
            </div>
          </IconRow>
        ))}
      </IconRowList>
    </div>
  );
};

export default NextSteps;
