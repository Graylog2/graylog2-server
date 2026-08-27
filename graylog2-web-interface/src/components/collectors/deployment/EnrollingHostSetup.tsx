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
import { Grid } from '@mantine/core';

import { Button } from 'components/bootstrap';
import { LinkContainer, Stack } from 'components/common';
import CollectorFactsSection from 'components/collectors/overview/onboarding/CollectorFactsSection';
import OnboardingTimeline from 'components/collectors/overview/onboarding/OnboardingTimeline';
import SourceStatusSection from 'components/collectors/overview/onboarding/SourceStatusSection';
import { useCollectorLogPreview } from 'components/collectors/hooks/useCollectorLogPreview';
import { useSources } from 'components/collectors/hooks/useSourceQueries';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import collectorReceivedMessagesUrl from 'components/collectors/common/collectorReceivedMessagesUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from 'components/collectors/common/fields';
import type { CollectorInstanceView } from 'components/collectors/types';

type Props = {
  instance: CollectorInstanceView;
  fleetName: string;
};

// Breathing room inside the expanded table row — table cells only bring their compact padding.
const SetupContainer = styled.div(
  ({ theme }) => css`
    padding: ${theme.spacings.md} ${theme.spacings.sm} ${theme.spacings.sm};
  `,
);

const TimelineContainer = styled.div`
  min-width: 300px;
`;

/**
 * Concise inline version of the onboarding connection-success view, embedded in the enrolling-
 * hosts list. Deliberately keeps the wizard page mounted (no navigation) so the generated token
 * and install command are not lost while verifying a host.
 */
const EnrollingHostSetup = ({ instance, fleetName }: Props) => {
  const { sourceLogs, sourceCounts } = useCollectorLogPreview(instance.instance_uid);
  const { data: sources } = useSources(instance.fleet_id);
  const sendTelemetry = useSendCollectorsTelemetry();

  const online = instance.status === 'online';
  const receiving = (sourceLogs?.total ?? 0) > 0;

  return (
    <SetupContainer>
      <Grid gutter="xl">
        <Grid.Col span="content">
          <TimelineContainer>
            <OnboardingTimeline
              instance={instance}
              fleetName={fleetName}
              sourceCount={sources?.length ?? 0}
              receivedTotal={sourceLogs?.total}
            />
          </TimelineContainer>
        </Grid.Col>
        <Grid.Col span="auto">
          <Stack gap="md">
            <CollectorFactsSection instance={instance} fleetName={fleetName} />
            <SourceStatusSection
              instance={instance}
              sources={sources}
              receiving={receiving}
              sourceCounts={sourceCounts}
            />
            {online && (
              <div>
                <LinkContainer to={collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid)}>
                  <Button
                    bsStyle="success"
                    bsSize="small"
                    onClick={() =>
                      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.LINK_CLICKED, {
                        app_action_value: 'deployment-open-in-search',
                        link: 'search',
                        instance_id: instance.instance_uid,
                      })
                    }>
                    Open in search
                  </Button>
                </LinkContainer>
              </div>
            )}
          </Stack>
        </Grid.Col>
      </Grid>
    </SetupContainer>
  );
};

export default EnrollingHostSetup;
