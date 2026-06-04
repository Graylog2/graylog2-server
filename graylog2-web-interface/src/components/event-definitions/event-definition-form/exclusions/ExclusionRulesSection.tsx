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

import { Button } from 'components/bootstrap';
import type { ExclusionRule, Matcher } from 'components/event-definitions/event-definitions-types';

import ExclusionRuleEditor from './ExclusionRuleEditor';

const Wrapper = styled.section`
  margin-top: 1rem;
  border-top: 1px solid ${({ theme }) => theme?.colors?.gray?.[90] ?? '#eee'};
  padding-top: 0.75rem;
`;

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
`;

const HeaderButton = styled.button`
  background: none;
  border: none;
  padding: 0;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
`;

const ErrorRoll = styled.div`
  color: ${({ theme }) => theme?.colors?.variant?.danger ?? '#a00'};
  font-size: 0.85em;
  margin-top: 0.25rem;
`;

const defaultRule = (): ExclusionRule => ({
  matchers: [{ type: 'ASSET', values: [] }],
});

const ruleHasError = (rule: ExclusionRule): boolean => {
  if (rule.matchers.length === 0) return true;

  return rule.matchers.some((m: Matcher) => {
    if (m.values.length === 0) return true;
    if (m.type === 'FIELD' && (!m.field_name || m.field_name.trim() === '')) return true;

    return false;
  });
};

type Props = {
  exclusions: ExclusionRule[];
  onChange: (next: ExclusionRule[]) => void;
};

const ExclusionRulesSection = ({ exclusions, onChange }: Props) => {
  const [expanded, setExpanded] = useState(false);

  const handleRuleChange = (idx: number, next: ExclusionRule) =>
    onChange(exclusions.map((r, i) => (i === idx ? next : r)));

  const handleRuleRemove = (idx: number) =>
    onChange(exclusions.filter((_, i) => i !== idx));

  const handleAddRule = () => {
    onChange([...exclusions, defaultRule()]);
    setExpanded(true);
  };

  const errorCount = exclusions.filter(ruleHasError).length;

  return (
    <Wrapper>
      <HeaderRow>
        <HeaderButton
          type="button"
          onClick={() => setExpanded((v) => !v)}
          aria-label="Exclusion rules">
          Exclusion rules ({exclusions.length})
        </HeaderButton>
        <Button bsSize="xsmall" onClick={handleAddRule} aria-label="Add rule">+ Add rule</Button>
      </HeaderRow>
      {errorCount > 0 && (
        <ErrorRoll>
          {errorCount} {errorCount === 1 ? 'rule has errors' : 'rules have errors'}
        </ErrorRoll>
      )}
      {expanded && exclusions.map((rule, idx) => (
        <ExclusionRuleEditor
          key={rule.id ?? idx}
          rule={rule}
          onChange={(next) => handleRuleChange(idx, next)}
          onRemove={() => handleRuleRemove(idx)} />
      ))}
    </Wrapper>
  );
};

export default ExclusionRulesSection;
