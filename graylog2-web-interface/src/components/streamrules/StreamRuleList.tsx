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
import PropTypes from 'prop-types';
import React from 'react';
import type { Stream, StreamRuleType } from 'src/stores/streams/StreamsStore';

import StreamRule from 'components/streamrules/StreamRule';
import { Spinner } from 'components/common';
import { ListGroup, ListGroupItem } from 'components/bootstrap';

type Props = {
  matchData: {
    matches: boolean,
    rules: { [id: string]: false },
  },
  onDelete: () => void,
  onSubmit: () => void,
  stream: Stream | undefined,
  streamRuleTypes: Array<StreamRuleType>,
}

const StreamRuleList = ({
  matchData,
  onDelete,
  onSubmit,
  stream,
  streamRuleTypes,
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
                    streamRule={streamRule}
                    streamRuleTypes={streamRuleTypes} />
      ))}

      {!hasStreamRules && <ListGroupItem>No rules defined.</ListGroupItem>}
    </ListGroup>
  );
};

StreamRuleList.propTypes = {
  matchData: PropTypes.shape({
    matches: PropTypes.bool,
    rules: PropTypes.object,
  }),
  onSubmit: PropTypes.func,
  onDelete: PropTypes.func,
  stream: PropTypes.object.isRequired,
  streamRuleTypes: PropTypes.array.isRequired,
};

StreamRuleList.defaultProps = {
  matchData: {},
  onSubmit: () => {},
  onDelete: () => {},
};

export default StreamRuleList;
