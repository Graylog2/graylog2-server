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
import { useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import { Icon, Link } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import Routes from 'routing/Routes';
import type { CollectorInstanceView } from 'components/collectors/types';

import { IconRow, IconRowList } from '../../common/IconRowList';
import collectorReceivedMessagesUrl from '../../common/collectorReceivedMessagesUrl';
import { AGENT_ID_FIELD } from '../../common/fields';

type Props = {
  instance: CollectorInstanceView;
  onLinkClick?: (link: string) => void;
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

// `title` and `description` are translation keys in the `collectors` namespace.
type NextLink = { key: string; icon: IconName; title: string; description: string; to: string };

const nextLinks = (instance: CollectorInstanceView): Array<NextLink> => [
  {
    key: 'search',
    icon: 'search',
    title: 'next_steps.explore',
    description: 'next_steps.explore_description',
    to: collectorReceivedMessagesUrl(AGENT_ID_FIELD, instance.instance_uid),
  },
  {
    key: 'configure-sources',
    icon: 'settings',
    title: 'next_steps.configure_sources',
    description: 'next_steps.configure_sources_description',
    to: Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id),
  },
  {
    key: 'fleets',
    icon: 'hub',
    title: 'next_steps.manage_fleets',
    description: 'next_steps.manage_fleets_description',
    to: Routes.SYSTEM.COLLECTORS.FLEETS,
  },
  {
    key: 'install-another',
    icon: 'add_circle',
    title: 'next_steps.install_another',
    description: 'next_steps.install_another_description',
    to: Routes.SYSTEM.COLLECTORS.OVERVIEW,
  },
];

/**
 * The panel below the onboarding timeline: pointers to what to do next while everything is
 * healthy, or recovery steps when the collector has gone offline.
 */
const NextSteps = ({ instance, onLinkClick = undefined }: Props) => {
  const { t } = useTranslation('collectors');
  const productName = useProductName();
  if (instance.status !== 'online') {
    return (
      <div>
        <PanelTitle>{t('next_steps.offline_title')}</PanelTitle>
        <TroubleshootingList>
          <li>{t('next_steps.check_service')}</li>
          <li>{t('next_steps.check_reachability', { productName })}</li>
          <li>{t('next_steps.check_logs')}</li>
        </TroubleshootingList>
      </div>
    );
  }

  return (
    <div>
      <PanelTitle>{t('next_steps.title')}</PanelTitle>
      <IconRowList>
        {nextLinks(instance).map(({ key, icon, title, description, to }) => (
          <IconRow key={key}>
            <Icon name={icon} />
            <div>
              <Link to={to} onClick={() => onLinkClick?.(key)}>
                {t(title)}
              </Link>
              <LinkDescription>{t(description)}</LinkDescription>
            </div>
          </IconRow>
        ))}
      </IconRowList>
    </div>
  );
};

export default NextSteps;
