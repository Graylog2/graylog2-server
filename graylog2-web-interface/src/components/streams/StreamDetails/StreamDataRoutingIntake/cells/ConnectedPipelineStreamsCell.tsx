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

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { StreamConnectedPipeline } from 'components/streams/StreamDetails/StreamDataRoutingIntake/types';

type Props = {
  streams: StreamConnectedPipeline['connected_streams'];
};

const ConnectedPipelineStreamsCell = ({ streams }: Props) => {
  if (!streams || streams.length < 1) return null;

  return (
    <>
      {streams.map((stream, index) => (
        <span key={stream.id}>
          <Link to={Routes.stream_view(stream.id)}>{stream.title}</Link>
          {index !== streams.length - 1 && ', '}
        </span>
      ))}
    </>
  );
};
export default ConnectedPipelineStreamsCell;
