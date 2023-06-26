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

import { Icon } from 'components/common';
import { Label } from 'components/bootstrap';
import { StreamsStore } from 'stores/streams/StreamsStore';
import type { Stream } from 'stores/streams/StreamsStore';

const StatusLabel = styled(Label)(({ $clickable }: { $clickable: boolean }) => css`
  cursor: ${$clickable ? 'pointer' : 'default'};
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`);

const Spacer = styled.div`
  border-left: 1px solid currentColor;
  height: 1em;
`;

const _title = (disabled: boolean, disabledChange: boolean, description: string) => {
  if (disabledChange) {
    return description;
  }

  return disabled ? 'Start stream' : 'Pause stream';
};

type Props = {
  stream: Stream,
};

const StatusCell = ({ stream }: Props) => {
  const disableChange = stream.is_default || !stream.is_editable;
  const description = stream.disabled ? 'Paused' : 'Running';
  const title = _title(stream.disabled, disableChange, description);

  const toggleStreamStatus = useCallback(async () => {
    if (stream.disabled) {
      await StreamsStore.resume(stream.id, (response) => response);
    }

    // eslint-disable-next-line no-alert
    if (!stream.disabled && window.confirm(`Do you really want to pause stream '${stream.title}'?`)) {
      await StreamsStore.pause(stream.id, (response) => response);
    }
  }, [stream.disabled, stream.id, stream.title]);

  return (
    <StatusLabel bsStyle={stream.disabled ? 'warning' : 'success'}
                 onClick={disableChange ? undefined : toggleStreamStatus}
                 title={title}
                 aria-label={title}
                 role="button"
                 $clickable={!disableChange}>
      {stream.disabled ? 'Paused' : 'Running'}
      {!disableChange && (
        <>
          <Spacer />
          <Icon name={stream.disabled ? 'play' : 'pause'} />
        </>
      )}
    </StatusLabel>
  );
};

export default StatusCell;
