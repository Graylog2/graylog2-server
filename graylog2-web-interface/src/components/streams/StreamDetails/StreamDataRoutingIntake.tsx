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

import type { Stream } from 'stores/streams/StreamsStore';
import SectionComponent from 'components/common/Section/SectionComponent';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import StreamRule from 'components/streamrules/StreamRule';

type Props = {
  stream: Stream,
}

const StreamDataRoutingInstake = ({ stream }: Props) => {
  const hasStreamRules = !!stream.rules?.length;

  return (
    <SectionComponent title="Data Routing - Intake">
      <ListGroup componentClass="ul">
        {hasStreamRules && stream.rules.map((streamRule) => (
          <StreamRule key={streamRule.id}
                      stream={stream}
                      streamRule={streamRule} />
        ))}

        {!hasStreamRules && <ListGroupItem>No rules defined.</ListGroupItem>}
      </ListGroup>
    </SectionComponent>
  );
};

export default StreamDataRoutingInstake;
