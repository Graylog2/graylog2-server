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
import { useCallback } from 'react';

import { isAnyPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import { Icon } from 'components/common';
import useStreamMutations from 'hooks/useStreamMutations';
import type { Stream } from 'logic/streams/types';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const StatusPill = styled.span<{ $success: boolean; $clickable: boolean }>(
  ({ theme, $success, $clickable }) => css`
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background-color: color-mix(in srgb, ${$success ? theme.colors.variant.success : theme.colors.variant.warning} 15%, transparent);
    border-radius: 4px;
    padding: 2px 8px;

    ${$clickable &&
    css`
      cursor: pointer;

      &:hover {
        background-color: color-mix(in srgb, ${$success ? theme.colors.variant.success : theme.colors.variant.warning} 25%, transparent);
      }
    `}
  `,
);

const Dot = styled.span<{ $success: boolean }>`
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background-color: ${({ theme, $success }) =>
    $success ? theme.colors.variant.success : theme.colors.variant.warning};
`;

const ToggleIcon = styled.span`
  display: flex;
  align-items: center;
  margin-top: 2px;
`;

const _title = (disabled: boolean, disabledChange: boolean, description: string) => {
  if (disabledChange) {
    return description;
  }

  return disabled ? 'Start stream' : 'Pause stream';
};

type Props = {
  stream: Stream;
};

const StatusCell = ({ stream }: Props) => {
  const currentUser = useCurrentUser();
  const userHasPermissions = isAnyPermitted(currentUser.permissions, [
    `streams:changestate:${stream.id}`,
    `streams:edit:${stream.id}`,
  ]);
  const disableChange = stream.is_default || !stream.is_editable || !userHasPermissions;
  const description = stream.disabled ? 'Paused' : 'Running';
  const title = _title(stream.disabled, disableChange, description);
  const sendTelemetry = useSendTelemetry();
  const { pauseStream, resumeStream } = useStreamMutations();

  const toggleStreamStatus = useCallback(async () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_STATUS_TOGGLED, {
      app_pathname: 'streams',
      app_action_value: 'stream-item-status',
      event_details: {
        enabled: !stream.disabled,
      },
    });

    // The api fns handle the error toast on rejection, so we swallow it here to avoid an unhandled rejection.
    if (stream.disabled) {
      await resumeStream(stream.id).catch(() => {});
    }

    // eslint-disable-next-line no-alert
    if (!stream.disabled && window.confirm(`Do you really want to pause stream '${stream.title}'?`)) {
      await pauseStream(stream.id).catch(() => {});
    }
  }, [sendTelemetry, stream.disabled, stream.id, stream.title, resumeStream, pauseStream]);

  return (
    <StatusPill
      onClick={disableChange ? undefined : toggleStreamStatus}
      title={title}
      aria-label={title}
      $success={!stream.disabled}
      $clickable={!disableChange}>
      <Dot $success={!stream.disabled} />
      {description}
      {!disableChange && (
        <ToggleIcon>
          <Icon name={stream.disabled ? 'play_arrow' : 'pause'} size="xs" />
        </ToggleIcon>
      )}
    </StatusPill>
  );
};

export default StatusCell;
