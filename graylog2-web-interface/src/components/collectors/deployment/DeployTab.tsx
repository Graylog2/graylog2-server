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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { Icon, Spinner, Timeline } from 'components/common';
import FleetChoice from 'components/collectors/overview/onboarding/FleetChoice';
import type { FleetChoiceValue } from 'components/collectors/overview/onboarding/FleetChoice';
import type { PlatformId } from 'components/collectors/overview/onboarding/platforms';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';

import EnrollingHostsList from './EnrollingHostsList';
import InstallStep from './InstallStep';
import TokenStep from './TokenStep';
import type { GeneratedToken } from './TokenStep';

import { useFleets } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

const StepBody = styled.div(
  ({ theme }) => css`
    padding: ${theme.spacings.md} 0 0 0;
  `,
);

const CheckBullet = () => <Icon name="check" size="sm" />;

const DeployTab = () => {
  const [fleetChoice, setFleetChoice] = useState<Fleet | null>(null);
  const [generatedToken, setGeneratedToken] = useState<GeneratedToken | null>(null);
  const [platformId, setPlatformId] = useState<PlatformId>('linux');
  const { data: fleets, isLoading: isFleetsLoading } = useFleets();
  const sendTelemetry = useSendCollectorsTelemetry();
  const history = useHistory();

  if (isFleetsLoading) return <Spinner />;

  // A lone fleet is auto-selected (same rule as FirstOnboarding.autoChoice); the box stays
  // visible so the user still sees which fleet the hosts will join.
  const resolvedFleet = fleetChoice ?? ((fleets?.length ?? 0) === 1 ? fleets![0] : null);

  const handleFleetSelect = (choice: FleetChoiceValue) => {
    if (choice.kind === 'create-new') {
      history.push(Routes.SYSTEM.COLLECTORS.FLEETS_NEW);

      return;
    }

    const fleet = fleets?.find((f) => f.id === choice.fleetId) ?? null;

    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED, {
      app_action_value: 'deployment-fleet',
      fleet_id: choice.fleetId,
    });

    setFleetChoice(fleet);
    setGeneratedToken(null); // fleet changed -> the previous token no longer applies
  };

  const handleChangeFleet = () => {
    setFleetChoice(null);
    setGeneratedToken(null);
  };

  const fleetStepDone = resolvedFleet ? 1 : 0;
  const activeStep = generatedToken ? 2 : fleetStepDone;

  return (
    <div>
      {/* Done steps get the filled check bullet; pending steps get Mantine's default hollow
          circle — same look as the first-run onboarding timeline. */}
      <Timeline active={activeStep} bulletSize={26} color="success">
        <Timeline.Item title="Select Fleet" bullet={resolvedFleet ? <CheckBullet /> : undefined}>
          <StepBody>
            <FleetChoice
              fleets={fleets ?? []}
              selectedFleet={resolvedFleet}
              onSelect={handleFleetSelect}
              onChange={handleChangeFleet}
              inline
            />
          </StepBody>
        </Timeline.Item>
        <Timeline.Item title="Generate Token" bullet={generatedToken ? <CheckBullet /> : undefined}>
          <StepBody>
            <TokenStep
              fleet={resolvedFleet}
              generatedToken={generatedToken}
              onGenerated={setGeneratedToken}
              onChangeToken={() => setGeneratedToken(null)}
            />
          </StepBody>
        </Timeline.Item>
        <Timeline.Item title="Install the Collector">
          <StepBody>
            <InstallStep token={generatedToken} platformId={platformId} onPlatformChange={setPlatformId} />
            {generatedToken && resolvedFleet && (
              <EnrollingHostsList
                key={generatedToken.token}
                fleetId={resolvedFleet.id}
                fleetName={resolvedFleet.name}
                platformId={platformId}
              />
            )}
          </StepBody>
        </Timeline.Item>
      </Timeline>
    </div>
  );
};

export default DeployTab;
