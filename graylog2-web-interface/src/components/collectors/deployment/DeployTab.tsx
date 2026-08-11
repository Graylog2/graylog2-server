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
import URI from 'urijs';

import { Icon, Spinner, Timeline } from 'components/common';
import FleetChoice from 'components/collectors/overview/onboarding/FleetChoice';
import type { FleetChoiceValue } from 'components/collectors/overview/onboarding/FleetChoice';
import type { PlatformId } from 'components/collectors/overview/onboarding/platforms';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useHistory from 'routing/useHistory';
import useQuery from 'routing/useQuery';
import Routes from 'routing/Routes';

import EnrollingHostsList from './EnrollingHostsList';
import InstallStep from './InstallStep';
import TokenStep from './TokenStep';
import type { GeneratedToken } from './TokenStep';

import { useFleets } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';

const StepBody = styled.div(
  ({ theme }) => css`
    padding: ${theme.spacings.md} 0 0 0;
  `,
);

const CheckBullet = () => <Icon name="check" size="sm" />;

const DeployTab = () => {
  // The selected fleet lives in the URL (?fleet=<id>) so other pages can deep-link into the
  // wizard with a fleet preselected. Same state-seeded-from-URL pattern as FleetDetail's tabs.
  const { fleet: fleetParam } = useQuery();
  const [selectedFleetId, setSelectedFleetId] = useState<string | null>(
    typeof fleetParam === 'string' ? fleetParam : null,
  );
  const [generatedToken, setGeneratedToken] = useState<GeneratedToken | null>(null);
  const [platformId, setPlatformId] = useState<PlatformId>('linux');
  const { data: fleets, isLoading: isFleetsLoading } = useFleets();
  const sendTelemetry = useSendCollectorsTelemetry();
  const history = useHistory();

  if (isFleetsLoading) return <Spinner />;

  // A lone fleet is auto-selected (same rule as FirstOnboarding.autoChoice); the box stays
  // visible so the user still sees which fleet the hosts will join.
  const resolvedFleet =
    (selectedFleetId ? fleets?.find((f) => f.id === selectedFleetId) : null) ??
    ((fleets?.length ?? 0) === 1 ? fleets![0] : null);

  const pushFleetUrl = (fleetId: string | null) => {
    let newUrl = new URI(window.location.href).removeSearch('fleet');
    if (fleetId) newUrl = newUrl.addSearch('fleet', fleetId);

    history.push(newUrl.resource());
  };

  const handleFleetSelect = (choice: FleetChoiceValue) => {
    if (choice.kind === 'create-new') {
      history.push(Routes.SYSTEM.COLLECTORS.FLEETS_NEW);

      return;
    }

    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED, {
      app_action_value: 'deployment-fleet',
      fleet_id: choice.fleetId,
    });

    setSelectedFleetId(choice.fleetId);
    setGeneratedToken(null); // fleet changed -> the previous token no longer applies
    pushFleetUrl(choice.fleetId);
  };

  const handleChangeFleet = () => {
    setSelectedFleetId(null);
    setGeneratedToken(null);
    pushFleetUrl(null);
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
