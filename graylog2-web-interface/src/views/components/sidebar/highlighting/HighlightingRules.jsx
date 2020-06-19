// @flow strict
import * as React from 'react';
import { useContext } from 'react';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';

import HighlightingRule, { HighlightingRuleGrid } from './HighlightingRule';
import ColorPreview from './ColorPreview';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

const HighlightingRules = () => {
  const rules = useContext(HighlightingRulesContext) ?? [];
  return (
    <>
      <SectionInfo>
        Search terms and field values can be highlighted. Highlighting your search query in the results can be enabled/disabled in the graylog server config.
        Any field value can be highlighted by clicking on the value and selecting &quot;Highlight this value&quot;.
      </SectionInfo>
      <SectionSubheadline>Active highlights</SectionSubheadline>
      <HighlightingRuleGrid>
        <ColorPreview color={DEFAULT_HIGHLIGHT_COLOR} />
        <div>Search terms</div>
      </HighlightingRuleGrid>

      {rules.map((rule) => <HighlightingRule key={`${rule.field}-${rule.value}-${rule.color}`} rule={rule} />)}
    </>
  );
};

export default HighlightingRules;
