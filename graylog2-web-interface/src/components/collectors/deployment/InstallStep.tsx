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
import MutedText from 'components/collectors/common/MutedText';
import InstallCommand from 'components/collectors/overview/onboarding/InstallCommand';
import PLATFORMS from 'components/collectors/overview/onboarding/platforms';
import type { PlatformId } from 'components/collectors/overview/onboarding/platforms';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { GeneratedToken } from './TokenStep';

import { useCollectorsConfig } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';

type Props = {
  token: GeneratedToken | null;
  platformId: PlatformId;
  onPlatformChange: (id: PlatformId) => void;
};

const InstallStep = ({ token, platformId, onPlatformChange }: Props) => {
  const { data: config } = useCollectorsConfig();
  const sendTelemetry = useSendCollectorsTelemetry();

  if (!token) {
    return <MutedText>Generate a token above to see the install command.</MutedText>;
  }

  return (
    <Tabs value={platformId} onChange={(value) => value && onPlatformChange(value as PlatformId)}>
      <Tabs.List>
        {PLATFORMS.map((platform) => (
          <Tabs.Tab key={platform.id} value={platform.id}>
            {platform.label}
          </Tabs.Tab>
        ))}
      </Tabs.List>
      {PLATFORMS.map((platform) => (
        <Tabs.Panel key={platform.id} value={platform.id}>
          {config && (
            <InstallCommand
              command={platform.commandTemplate(config.http.hostname, config.http.port, token.token)}
              platformLabel={platform.label}
              tokenDuration={token.expiresIn}
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
          )}
        </Tabs.Panel>
      ))}
    </Tabs>
  );
};

export default InstallStep;
