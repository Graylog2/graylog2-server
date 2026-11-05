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
import moment from 'moment';
import styled, { css } from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Icon, Link, RelativeTime, Spinner, Timeline } from 'components/common';
import Routes from 'routing/Routes';
import type { CollectorInstanceView } from 'components/collectors/types';
import { PREVIEW_RANGE_SECONDS } from 'components/collectors/hooks/useCollectorLogPreview';

type Props = {
  instance: CollectorInstanceView;
  fleetName: string | undefined;
  sourceCount: number;
  /** Source messages received in the preview window; undefined while the first search is running. */
  receivedTotal: number | undefined;
  onFleetLinkClick?: () => void;
};

const StepDetail = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
  `,
);

const CheckBullet = () => <Icon name="check" size="sm" />;

/**
 * The onboarding journey as a guided timeline: enrolled → connected → sources configured → first
 * messages. The last step is the only one that can still be in flight — it spins until the log
 * preview sees source messages, completes once messages arrive, and turns into a warning when the
 * collector drops offline before delivering any.
 */
const OnboardingTimeline = ({
  instance,
  fleetName,
  sourceCount,
  receivedTotal,
  onFleetLinkClick = undefined,
}: Props) => {
  const { t } = useTranslation('collectors');
  const offline = instance.status !== 'online';
  const receiving = (receivedTotal ?? 0) > 0;
  // Steps 0-2 are always in the past for an enrolled collector; step 3 joins them once messages
  // flow (or fails visibly when the collector goes dark).
  const activeStep = receiving || offline ? 3 : 2;

  const lastStep = () => {
    if (offline) {
      return (
        <Timeline.Item
          title={t('timeline.connection_lost')}
          color="warning"
          bullet={<Icon name="priority_high" size="sm" />}>
          <StepDetail>
            <Trans
              t={t}
              i18nKey="timeline.last_heartbeat"
              components={{ lastSeen: <RelativeTime dateTime={instance.last_seen} /> }}
            />
          </StepDetail>
        </Timeline.Item>
      );
    }

    if (receiving) {
      return (
        <Timeline.Item title={t('timeline.first_messages')} bullet={<CheckBullet />}>
          <StepDetail>
            {t('timeline.receiving', {
              count: receivedTotal,
              duration: moment.duration(PREVIEW_RANGE_SECONDS, 'seconds').humanize(),
            })}
          </StepDetail>
        </Timeline.Item>
      );
    }

    return (
      <Timeline.Item title={t('timeline.first_messages')}>
        <StepDetail>
          <Spinner text={t('timeline.listening')} delay={0} />
        </StepDetail>
      </Timeline.Item>
    );
  };

  return (
    <Timeline active={activeStep} bulletSize={26} color="success">
      <Timeline.Item title={t('timeline.enrolled')} bullet={<CheckBullet />}>
        <StepDetail>
          <Trans
            t={t}
            i18nKey="timeline.enrolled_detail"
            components={{ enrolledAt: <RelativeTime dateTime={instance.enrolled_at} /> }}
          />
        </StepDetail>
      </Timeline.Item>
      <Timeline.Item title={t('timeline.connected')} bullet={<CheckBullet />}>
        <StepDetail>
          <Trans
            t={t}
            i18nKey="timeline.connected_detail"
            components={{ lastSeen: <RelativeTime dateTime={instance.last_seen} /> }}
          />
        </StepDetail>
      </Timeline.Item>
      <Timeline.Item title={t('timeline.sources_configured')} bullet={<CheckBullet />}>
        <StepDetail>
          <Trans
            t={t}
            i18nKey="timeline.sources_detail"
            count={sourceCount}
            values={{ fleetName: fleetName ?? t('os.unknown') }}
            components={{
              fleetLink: <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)} onClick={onFleetLinkClick} />,
            }}
          />
        </StepDetail>
      </Timeline.Item>
      {lastStep()}
    </Timeline>
  );
};

export default OnboardingTimeline;
