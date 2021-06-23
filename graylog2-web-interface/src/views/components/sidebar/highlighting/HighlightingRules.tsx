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
import { useContext, useState } from 'react';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import IconButton from 'components/common/IconButton';

import HighlightingRule, { HighlightingRuleGrid, RuleContainer } from './HighlightingRule';
import ColorPreview from './ColorPreview';
import HighlightForm from './HighlightForm';

import SectionInfo from '../SectionInfo';
import SectionSubheadline from '../SectionSubheadline';

const HighlightingRules = () => {
  const [showForm, setShowForm] = useState(false);
  const rules = useContext(HighlightingRulesContext) ?? [];

  return (
    <>
      <SectionInfo>
        Search terms and field values can be highlighted. Highlighting your search query in the results can be enabled/disabled in the graylog server config.
        Any field value can be highlighted by clicking on the value and selecting &quot;Highlight this value&quot;.
        If a term or a value has more than one rule, the last matching rule is used.
      </SectionInfo>
      <SectionSubheadline>Active highlights <IconButton className="pull-right" name="plus" onClick={() => setShowForm(!showForm)} /> </SectionSubheadline>
      { showForm && <HighlightForm onClose={() => setShowForm(false)} />}
      <HighlightingRuleGrid>
        <ColorPreview color={DEFAULT_HIGHLIGHT_COLOR} />
        <RuleContainer>Search terms</RuleContainer>
      </HighlightingRuleGrid>
      {rules.map((rule) => <HighlightingRule key={`${rule.field}-${rule.value}-${rule.color}-${rule.condition}`} rule={rule} />)}
    </>
  );
};

export default HighlightingRules;
