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
import React from 'react';

import { Button } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import type { Input } from 'components/messageloaders/Types';
import { LinkContainer } from 'components/common/router';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import recentMessagesTimeRange from 'util/TimeRangeHelper';
import { getPathnameWithoutId } from 'util/URLUtils';
import useCurrentUser from 'hooks/useCurrentUser';
import Routes from 'routing/Routes';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';

type Props = {
  input: Input,
}

const ShowReceivedMessagesButton = ({ input }: Props) => {
  const currentUser = useCurrentUser();
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  const queryField = (input?.type === 'org.graylog.plugins.forwarder.input.ForwarderServiceInput') ? 'gl2_forwarder_input' : 'gl2_source_input';

  if (input?.id && isPermitted(currentUser.permissions, ['searches:relative'])) {
    return (
      <LinkContainer key={`received-messages-${input.id}`}
                     to={Routes.search(`${queryField}:${input.id}`, recentMessagesTimeRange())}>
        <Button onClick={() => {
          sendTelemetry(TELEMETRY_EVENT_TYPE.INPUTS.SHOW_RECEIVED_MESSAGES_CLICKED, {
            app_pathname: getPathnameWithoutId(pathname),
            app_action_value: 'show-received-messages',
          });
        }}>
          Show received messages
        </Button>
      </LinkContainer>
    );
  }

  return null;
};

export default ShowReceivedMessagesButton;
