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

import { type Stream } from 'stores/streams/StreamsStore';
import { Table } from 'components/bootstrap';
import DetailsStreamRule from 'components/streamrules/DetailsStreamRule';
import { IfPermitted, Section } from 'components/common';
import CreateStreamRuleButton from 'components/streamrules/CreateStreamRuleButton';

type Props = {
  stream: Stream,
}

export const Headline = styled.h2(({ theme }) => css`
  margin-top: ${theme.spacings.sm};
  margin-bottom: ${theme.spacings.xs};
`);

const StreamDataRoutingInstake = ({ stream }: Props) => {
  const hasStreamRules = !!stream.rules?.length;

  return (
    <Section title="Stream rules"
             actions={(
               <IfPermitted permissions="streams:create">
                 <CreateStreamRuleButton bsStyle="success"
                                         streamId={stream.id} />
               </IfPermitted>
             )}>
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
  );
};

export default StreamDataRoutingInstake;
