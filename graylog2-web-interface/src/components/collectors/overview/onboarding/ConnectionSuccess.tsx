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
import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import styled, { css } from 'styled-components';
import { Grid } from '@mantine/core';
import moment from 'moment/moment';

import { Button } from 'components/bootstrap';
import { Group, LinkContainer, RelativeTime, Stack } from 'components/common';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { TelemetryEventType } from 'logic/telemetry/TelemetryContext';
import Routes from 'routing/Routes';
import type { CollectorInstanceView } from 'components/collectors/types';
import { useSources } from 'components/collectors/hooks/useSourceQueries';
import { useCollectorLogPreview, PREVIEW_RANGE_SECONDS } from 'components/collectors/hooks/useCollectorLogPreview';
import { instanceKeyFn } from 'components/collectors/hooks/useInstanceQueries';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { instanceTelemetryProps } from 'components/collectors/hooks/telemetry-helpers';

import LogPreviewSection from './LogPreviewSection';
import OnboardingTimeline from './OnboardingTimeline';
import NextSteps from './NextSteps';
import CollectorFactsSection from './CollectorFactsSection';
import SourceStatusSection from './SourceStatusSection';

import InstanceStatusLabel from '../../common/InstanceStatusLabel';
import collectorReceivedMessagesUrl from '../../common/collectorReceivedMessagesUrl';
import collectorSystemLogsUrl from '../../common/collectorSystemLogsUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from '../../common/fields';

type Props = {
  instance: CollectorInstanceView;
  fleetName: string | undefined;
};

const Title = styled.h2`
  margin: 0;
`;

const Subtitle = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.xxs};
    color: ${theme.colors.text.secondary};
  `,
);

// The timeline and next-steps rails keep a readable width; the detail column takes what is left.
const ColContainer = styled.div`
  min-width: 350px;
`;

type OnboardingOutcome = 'offline' | 'online-silent' | 'online-receiving';

// One event per onboarding state, so entering a state always reports the same way.
const ONBOARDING_STATE_EVENTS: Record<OnboardingOutcome, { eventType: TelemetryEventType; appActionValue: string }> = {
  offline: {
    eventType: TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.CONNECTION_LOST,
    appActionValue: 'onboarding-connection-lost',
  },
  'online-silent': {
    eventType: TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.AWAITING_DATA,
    appActionValue: 'onboarding-awaiting-data',
  },
  'online-receiving': {
    eventType: TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.COMPLETED,
    // Unchanged from before the state machine, so existing dashboards keep working.
    appActionValue: 'collector-onboarding-completed',
  },
};

const ConnectionSuccess = ({ instance, fleetName }: Props) => {
  const { selfLogs, sourceLogs, sourceCounts, selfLogsError, sourceLogsError, isLoading } = useCollectorLogPreview(
    instance.instance_uid,
  );
  const { data: sources } = useSources(instance.fleet_id);
  const queryClient = useQueryClient();
  const sendTelemetry = useSendCollectorsTelemetry();

  const online = instance.status === 'online';
  const receiving = (sourceLogs?.total ?? 0) > 0;
  const sourceLogsUrl = collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid);

  // Which of the page's three states the user is looking at; attached to every click event so
  // interactions can be segmented by how the onboarding actually went.
  let outcome: OnboardingOutcome = 'online-silent';
  if (!online) outcome = 'offline';
  else if (receiving) outcome = 'online-receiving';

  // Onboarding is a three-state machine, and every entry into a state is worth an event.
  // Emitting on *state entry* rather than on a condition being true is what stops the
  // heartbeat-interval polling from re-firing events, and it is what makes a reconnect
  // visible: offline -> online-silent is a real transition even though no data has arrived.
  // `from_outcome` carries where we came from, so a funnel can be reconstructed from the
  // events alone.
  const previousOutcome = useRef<OnboardingOutcome | null>(null);
  const completedReportedFor = useRef<string | null>(null);

  useEffect(() => {
    if (previousOutcome.current === outcome) return;

    const fromOutcome = previousOutcome.current;
    previousOutcome.current = outcome;

    const { eventType, appActionValue } = ONBOARDING_STATE_EVENTS[outcome];

    // `Completed` is a funnel milestone rather than a state: a collector that drops and
    // recovers re-enters the receiving state, but it has not onboarded a second time.
    if (eventType === TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.COMPLETED) {
      if (completedReportedFor.current === instance.instance_uid) return;

      completedReportedFor.current = instance.instance_uid;
    }

    sendTelemetry(eventType, {
      app_action_value: appActionValue,
      ...instanceTelemetryProps(instance),
      outcome,
      from_outcome: fromOutcome,
      // `outcome` alone cannot say whether messages ever arrived -- it collapses to 'offline'
      // either way. This separates "worked, then died" from "never delivered anything".
      // Scoped to the log-preview window, so it goes false once the last messages age out.
      had_messages: receiving,
      seconds_since_last_seen: moment().diff(moment(instance.last_seen), 'seconds'),
    });
  }, [outcome, receiving, instance, sendTelemetry]);

  const reportNextStep = (appActionValue: string, link: string) =>
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.NEXT_STEP_CLICKED, {
      app_action_value: appActionValue,
      link,
      outcome,
    });

  const subtitle = () => {
    if (!online)
      return (
        <>
          The collector connected once but hasn&apos;t reported in since <RelativeTime dateTime={instance.last_seen} />.
        </>
      );
    if (receiving) return <>The collector is connected and delivering messages.</>;

    return <>Almost there &mdash; the collector is connected and we&apos;re listening for its first messages.</>;
  };

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="flex-start">
        <div>
          <Group gap="sm">
            <Title>Setting Up {instance.hostname ?? instance.instance_uid}</Title>
            {!online && <InstanceStatusLabel status={instance.status} />}
          </Group>
          <Subtitle>{subtitle()}</Subtitle>
        </div>
        {online ? (
          <LinkContainer to={sourceLogsUrl}>
            <Button bsStyle="success" onClick={() => reportNextStep('onboarding-open-in-search', 'search')}>
              Open in search
            </Button>
          </LinkContainer>
        ) : (
          <Group gap="xs">
            <LinkContainer to={Routes.SYSTEM.COLLECTORS.INSTANCES}>
              <Button onClick={() => reportNextStep('onboarding-view-instances', 'instances')}>View instances</Button>
            </LinkContainer>
            <Button
              bsStyle="info"
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.CHECK_AGAIN_CLICKED, {
                  app_action_value: 'onboarding-check-again',
                  status: instance.status,
                });

                queryClient.invalidateQueries({ queryKey: instanceKeyFn(instance.instance_uid) });
              }}>
              Check again
            </Button>
          </Group>
        )}
      </Group>

      <Grid gutter="xl">
        <Grid.Col span="content">
          <ColContainer>
            <OnboardingTimeline
              instance={instance}
              fleetName={fleetName}
              sourceCount={sources?.length ?? 0}
              receivedTotal={sourceLogs?.total}
              onFleetLinkClick={() => reportNextStep('onboarding-fleet-link', 'fleet')}
            />
          </ColContainer>
        </Grid.Col>

        <Grid.Col span="auto">
          <Stack gap="md">
            <CollectorFactsSection
              instance={instance}
              fleetName={fleetName}
              onFleetLinkClick={() => reportNextStep('onboarding-fleet-link', 'fleet')}
            />
            <SourceStatusSection
              instance={instance}
              sources={sources}
              receiving={receiving}
              sourceCounts={sourceCounts}
              onConfigureSources={() => reportNextStep('onboarding-configure-sources', 'configure-sources')}
            />
          </Stack>
        </Grid.Col>
        <Grid.Col span="content">
          <ColContainer>
            <NextSteps instance={instance} onLinkClick={(link) => reportNextStep('onboarding-next-step', link)} />
          </ColContainer>
        </Grid.Col>
      </Grid>

      {/* While healthy the preview tails what the collector delivers; once it drops offline the
          collector's own logs usually hold the reason, so they take over. */}
      {online ? (
        <LogPreviewSection
          title="Log Preview"
          searchUrl={sourceLogsUrl}
          preview={sourceLogs}
          isLoading={isLoading}
          error={sourceLogsError}
          caption={`Showing messages received since ${moment.duration(PREVIEW_RANGE_SECONDS, 'seconds').humanize()}${receiving ? '' : ' - checking every few seconds'}`}
          onOpenSearch={() => reportNextStep('onboarding-log-preview-search', 'log-preview')}
        />
      ) : (
        <LogPreviewSection
          title="Log Preview"
          searchUrl={collectorSystemLogsUrl(instance.instance_uid)}
          preview={selfLogs}
          isLoading={isLoading}
          error={selfLogsError}
          caption={`Showing Collector system messages received since ${moment.duration(PREVIEW_RANGE_SECONDS, 'seconds').humanize()}${receiving ? '' : ' - checking every few seconds'}`}
          onOpenSearch={() => reportNextStep('onboarding-log-preview-search', 'log-preview')}
        />
      )}
    </Stack>
  );
};

export default ConnectionSuccess;
