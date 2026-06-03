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
import styled from 'styled-components';

import type { ExclusionRule, Matcher } from 'components/event-definitions/event-definitions-types';

const Wrapper = styled.dl`
  margin: 0;
`;

const RuleTitle = styled.dt`
  font-weight: 600;
  margin-top: 0.5rem;
`;

const RuleBody = styled.dd`
  margin: 0 0 0.25rem 1rem;
`;

const matcherToString = (m: Matcher): string => {
  const head = m.type === 'FIELD' && m.field_name
    ? `FIELD(${m.field_name})`
    : m.type;

  return `${head} IN [${m.values.join(', ')}]`;
};

type Props = { exclusions?: ExclusionRule[] };

const ExclusionRulesSummary = ({ exclusions = [] }: Props) => {
  if (exclusions.length === 0) {
    return null;
  }

  return (
    <Wrapper data-testid="exclusion-rules-summary">
      {exclusions.map((rule, ruleIdx) => {
        const ruleKey = rule.id ?? `rule-${ruleIdx}`;

        return (
          <React.Fragment key={ruleKey}>
            <RuleTitle>{rule.title?.trim() || 'Unnamed rule'}</RuleTitle>
            <RuleBody>
              {rule.matchers.map((m, idx) => {
                const matcherKey = `${ruleKey}-${m.type}-${m.field_name ?? ''}-${m.values.join('|')}`;

                return (
                  <React.Fragment key={matcherKey}>
                    {idx > 0 && <span> AND </span>}
                    <span>{matcherToString(m)}</span>
                  </React.Fragment>
                );
              })}
            </RuleBody>
          </React.Fragment>
        );
      })}
    </Wrapper>
  );
};

export default ExclusionRulesSummary;
