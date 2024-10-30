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
