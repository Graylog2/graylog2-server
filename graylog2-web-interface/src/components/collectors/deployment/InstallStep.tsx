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

import { Tabs } from 'components/bootstrap';
import { ClipboardButton } from 'components/common';
import enrollEndpointUrl from 'components/collectors/common/enrollEndpointUrl';
import MutedText from 'components/collectors/common/MutedText';
import InstallCommand from 'components/collectors/overview/onboarding/InstallCommand';
import PLATFORMS from 'components/collectors/overview/onboarding/platforms';
import type { PlatformId } from 'components/collectors/overview/onboarding/platforms';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { GeneratedToken } from './TokenStep';

import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';

type Props = {
  token: GeneratedToken | null;
  platformId: PlatformId;
  onPlatformChange: (id: PlatformId) => void;
};

const InstallStep = ({ token, platformId, onPlatformChange }: Props) => {
  const sendTelemetry = useSendCollectorsTelemetry();

  if (!token) {
    return <MutedText>Generate a token above to see the install command.</MutedText>;
  }

  const handlePlatformChange = (value: string | null) => {
    if (!value) return;

    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTALL.PLATFORM_SELECTED, {
      app_action_value: 'deployment-platform',
      platform: value,
    });

    onPlatformChange(value as PlatformId);
  };

  return (
    <Tabs value={platformId} onChange={handlePlatformChange}>
      <Tabs.List>
        {PLATFORMS.map((platform) => (
          <Tabs.Tab key={platform.id} value={platform.id}>
            {platform.label}
          </Tabs.Tab>
        ))}
      </Tabs.List>
      {PLATFORMS.map((platform) => (
        <Tabs.Panel key={platform.id} value={platform.id}>
          <InstallCommand
            command={platform.commandTemplate(enrollEndpointUrl(), token.token)}
            platformLabel={platform.label}
            tokenDuration={token.expiresIn}
            onCopySuccess={() =>
              sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTALL.COMMAND_COPIED, {
                app_action_value: 'deployment-copy-command',
                platform: platform.id,
              })
            }
            actions={
              <ClipboardButton
                text={token.token}
                title="Copy token only"
                bsSize="sm"
                onSuccess={() =>
                  sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.TOKEN_COPIED, {
                    app_action_value: 'deployment-copy-token',
                  })
                }
              />
            }
          />
        </Tabs.Panel>
      ))}
    </Tabs>
  );
};

export default InstallStep;
