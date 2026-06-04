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
import { useState } from 'react';
import styled from 'styled-components';

import { Button, Input } from 'components/bootstrap';
import type { ExclusionRule, Matcher } from 'components/event-definitions/event-definitions-types';

import MatcherEditor from './MatcherEditor';

const Wrapper = styled.div`
  border: 1px solid ${({ theme }) => theme?.colors?.gray?.[90] ?? '#ddd'};
  border-radius: 4px;
  padding: 0.75rem;
  margin-bottom: 0.75rem;
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
`;

const ErrorText = styled.div`
  color: ${({ theme }) => theme?.colors?.variant?.danger ?? '#a00'};
  font-size: 0.85em;
  margin-top: 0.25rem;
`;

const defaultMatcher = (): Matcher => ({ type: 'ASSET', values: [] });

type Props = {
  rule: ExclusionRule;
  onChange: (next: ExclusionRule) => void;
  onRemove: () => void;
};

const ExclusionRuleEditor = ({ rule, onChange, onRemove }: Props) => {
  const propTitle = rule.title ?? '';
  const [titleDraft, setTitleDraft] = useState<string>(propTitle);
  const [lastPropTitle, setLastPropTitle] = useState<string>(propTitle);

  if (propTitle !== lastPropTitle) {
    setLastPropTitle(propTitle);
    setTitleDraft(propTitle);
  }

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTitleDraft(e.target.value);
    onChange({ ...rule, title: e.target.value });
  };

  const handleMatcherChange = (idx: number, next: Matcher) =>
    onChange({ ...rule, matchers: rule.matchers.map((m, i) => (i === idx ? next : m)) });

  const handleMatcherRemove = (idx: number) =>
    onChange({ ...rule, matchers: rule.matchers.filter((_, i) => i !== idx) });

  const handleAddMatcher = () =>
    onChange({ ...rule, matchers: [...rule.matchers, defaultMatcher()] });

  return (
    <Wrapper>
      <Header>
        <Input
          id={`rule-title-${rule.id ?? 'new'}`}
          type="text"
          label="Title"
          aria-label="Rule title"
          value={titleDraft}
          onChange={handleTitleChange} />
        <Button bsSize="xsmall" onClick={onRemove} aria-label="Remove rule">Remove rule</Button>
      </Header>
      {rule.matchers.map((m, idx) => (
        <MatcherEditor
          // eslint-disable-next-line react/no-array-index-key
          key={idx}
          matcher={m}
          onChange={(next) => handleMatcherChange(idx, next)}
          onRemove={() => handleMatcherRemove(idx)} />
      ))}
      <Button bsSize="xsmall" onClick={handleAddMatcher} aria-label="Add matcher">+ Add matcher</Button>
      {rule.matchers.length === 0 && <ErrorText>Rule must have at least one matcher.</ErrorText>}
    </Wrapper>
  );
};

export default ExclusionRuleEditor;
