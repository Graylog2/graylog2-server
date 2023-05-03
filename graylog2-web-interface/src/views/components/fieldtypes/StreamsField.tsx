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
