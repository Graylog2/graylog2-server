// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import styled from 'styled-components';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';

import HighlightingRule, { HighlightingRuleGrid } from './HighlightingRule';
import ColorPreview from './ColorPreview';

const Headline = styled.h4`
  margin-bottom: 10px;
`;

const HighlightingRules = () => {
  const rules = useContext(HighlightingRulesContext) ?? [];
  return (
    <>
      <Headline>Highlighting</Headline>
      <HighlightingRuleGrid>
        <ColorPreview color={DEFAULT_HIGHLIGHT_COLOR} />
        <div>Search terms</div>
      </HighlightingRuleGrid>

      {rules.map((rule) => <HighlightingRule key={`${rule.field}-${rule.value}-${rule.color}`} rule={rule} />)}
    </>
  );
};

export default HighlightingRules;
