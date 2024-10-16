import React from 'react';
import type { Stream, MatchData } from 'src/stores/streams/StreamsStore';

import StreamRule from 'components/streamrules/StreamRule';
import { Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';

type Props = {
  matchData?: MatchData
  onDelete?: (ruleId: string) => void
  onSubmit?: (ruleId: string, data: unknown) => void
  stream: Stream | undefined,
}

const StreamRuleList = ({
  matchData,
  onDelete = () => {},
  onSubmit = () => {},
  stream,
}: Props) => {
  if (!stream) {
    return <Spinner />;
  }

  const hasStreamRules = !!stream.rules.length;

  return (
    <ListGroup componentClass="ul">
      {hasStreamRules && stream.rules.map((streamRule) => (
        <StreamRule key={streamRule.id}
                    matchData={matchData}
                    onSubmit={onSubmit}
                    onDelete={onDelete}
                    stream={stream}
                    streamRule={streamRule} />
      ))}

      {!hasStreamRules && <ListGroupItem>No rules defined.</ListGroupItem>}
    </ListGroup>
  );
};

export default StreamRuleList;
