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
