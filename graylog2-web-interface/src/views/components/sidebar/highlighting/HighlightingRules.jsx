// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import connect from 'stores/connect';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import { HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';
import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';

import HighlightingRule, { HighlightingRuleGrid } from './HighlightingRule';
import ColorPreview from './ColorPreview';

const Headline = styled.h4`
  margin-bottom: 10px;
`;

type Props = {
  rules: Array<Rule>,
};

const HighlightingRules = ({ rules = [] }: Props) => {
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

HighlightingRules.propTypes = {
  rules: PropTypes.arrayOf(PropTypes.instanceOf(Rule)),
};

HighlightingRules.defaultProps = {
  rules: [],
};

export default connect(HighlightingRules, { rules: HighlightingRulesStore });
