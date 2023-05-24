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
import { useCallback, useContext, useMemo } from 'react';
import styled from 'styled-components';

import StreamsContext from 'contexts/StreamsContext';

type Props = {
  value: string,
}

const StreamsList = styled.span`
  span:not(:last-child)::after {
    content: ", ";
  }
`;

const StreamsField = ({ value }: Props) => {
  const streams = useContext(StreamsContext);
  const streamsMap = useMemo(() => Object.fromEntries(streams.map((stream) => [stream.id, stream]) ?? []), [streams]);
  const renderStream = useCallback((streamId: string) => <span title={streamId}>{streamsMap[streamId]?.title ?? streamId}</span>, [streamsMap]);

  return Array.isArray(value)
    ? <StreamsList>{value.map(renderStream)}</StreamsList>
    : renderStream(value);
};

export default StreamsField;
