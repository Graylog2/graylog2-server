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

import { useRef } from 'react';
import * as React from 'react';

import StreamCountBadge from 'components/streams/StreamCountBadge';
import type { Stream } from 'stores/streams/StreamsStore';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';

type Props = {
  stream: Stream
}

const PipelinesCell = ({ stream }: Props) => {
  const buttonRef = useRef();
  const { data } = usePipelinesConnectedStream(stream.id);

  if (stream.is_default || !stream.is_editable) {
    return null;
  }

  const pipelinesCount = data?.length || 0;

  return (
    <StreamCountBadge $disabled={pipelinesCount === 0} ref={buttonRef} title="Connected pipelines">
      {pipelinesCount}
    </StreamCountBadge>
  );
};

export default PipelinesCell;
