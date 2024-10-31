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
import { useQueryClient } from '@tanstack/react-query';

import { type Stream } from 'stores/streams/StreamsStore';
import { Alert, Table } from 'components/bootstrap';
import DetailsStreamRule from 'components/streamrules/DetailsStreamRule';
import { IfPermitted, Section } from 'components/common';
import CreateStreamRuleButton from 'components/streamrules/CreateStreamRuleButton';
import MatchingTypeSwitcher from 'components/streams/MatchingTypeSwitcher';

type Props = {
  stream: Stream,
}

export const Headline = styled.h2(({ theme }) => css`
  margin-top: ${theme.spacings.sm};
  margin-bottom: ${theme.spacings.xs};
`);

const StreamDataRoutingInstake = ({ stream }: Props) => {
  const queryClient = useQueryClient();

  const hasStreamRules = !!stream.rules?.length;
  const isDefaultStream = stream.is_default;
  const isNotEditable = !stream.is_editable;

  const handleMatchingTypeSwitched = () => {
    queryClient.invalidateQueries(['stream', stream.id]);
  };

  return (
    <>
      <Alert bsStyle="default">
        Stream Rules take effect first in the default processing order, and are used to direct messages from Inputs into Streams.
        Any message that meets the criteria of the Stream Rule(s) will be directed into this Stream.
      </Alert>

      <Section title="Stream rules"
               actions={(
                 <IfPermitted permissions={`streams:edit:${stream.id}`}>
                   <CreateStreamRuleButton bsStyle="success"
                                           disabled={isDefaultStream || isNotEditable}
                                           streamId={stream.id} />
                 </IfPermitted>
             )}>
        <MatchingTypeSwitcher stream={stream} onChange={handleMatchingTypeSwitched} />
        <Table condensed striped hover>
          <thead>
            <tr>
              <th colSpan={2}>Rule</th>
            </tr>
          </thead>
          <tbody>
            {hasStreamRules && stream.rules.map((streamRule) => (
              <DetailsStreamRule key={streamRule.id}
                                 stream={stream}
                                 streamRule={streamRule} />
            ))}

            {!hasStreamRules && (
            <tr>
              <td>No rules defined.</td>
            </tr>
            )}
          </tbody>
        </Table>
      </Section>
    </>
  );
};

export default StreamDataRoutingInstake;
