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
import React, { useCallback } from 'react';

import {
  updateHighlightingRules,
  addHighlightingRule,
  updateHighlightingRule, removeHighlightingRule,
} from 'views/logic/slices/highlightActions';
import HighlightingRules from 'views/components/sidebar/highlighting/HighlightingRules';
import type {
  Value,
  Condition,
  Color,
} from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import useAppDispatch from 'stores/useAppDispatch';

const DESCRIPTION = 'Search terms and field values can be highlighted. Highlighting your search query in the results can be enabled/disabled in the graylog server config.\n'
  + 'Any field value can be highlighted by clicking on the value and selecting "Highlight this value".\n'
  + 'If a term or a value has more than one rule, the first matching rule is used.';

const ViewsHighlightingRules = () => {
  const dispatch = useAppDispatch();
  const onUpdateRules = useCallback((newRules: Array<HighlightingRule>) => dispatch(updateHighlightingRules(newRules)).then(() => {}), [dispatch]);

  const onCreateRule = useCallback((newRule: HighlightingRule) => (
    dispatch(addHighlightingRule(newRule)).then(() => {})
  ), [dispatch]);

  const onUpdateRule = useCallback((targetRule: HighlightingRule, field: string, value: Value, condition: Condition, color: Color) => (
    dispatch(updateHighlightingRule(targetRule, { field, value, condition, color })).then(() => {})
  ), [dispatch]);

  const onDeleteRule = useCallback((rule: HighlightingRule) => (
    dispatch(removeHighlightingRule(rule)).then(() => {})
  ), [dispatch]);

  return (
    <HighlightingRules description={DESCRIPTION}
                       onDeleteRule={onDeleteRule}
                       onUpdateRules={onUpdateRules}
                       onCreateRule={onCreateRule}
                       onUpdateRule={onUpdateRule} />
  );
};

export default ViewsHighlightingRules;
