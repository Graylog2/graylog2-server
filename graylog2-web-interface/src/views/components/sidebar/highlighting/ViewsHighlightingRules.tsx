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
import { useTranslation } from 'react-i18next';

import {
  updateHighlightingRules,
  addHighlightingRule,
  updateHighlightingRule,
  removeHighlightingRule,
} from 'views/logic/slices/highlightActions';
import HighlightingRules from 'views/components/sidebar/highlighting/HighlightingRules';
import type { Value, Condition, Color } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useProductName from 'brand-customization/useProductName';

const ViewsHighlightingRules = () => {
  const dispatch = useViewsDispatch();
  const productName = useProductName();
  const { t } = useTranslation('search');
  const onUpdateRules = useCallback(
    (newRules: Array<HighlightingRule>) => dispatch(updateHighlightingRules(newRules)).then(() => {}),
    [dispatch],
  );

  const onCreateRule = useCallback(
    (newRule: HighlightingRule) => dispatch(addHighlightingRule(newRule)).then(() => {}),
    [dispatch],
  );

  const onUpdateRule = useCallback(
    (targetRule: HighlightingRule, field: string, value: Value, condition: Condition, color: Color) =>
      dispatch(updateHighlightingRule(targetRule, { field, value, condition, color })).then(() => {}),
    [dispatch],
  );

  const onDeleteRule = useCallback(
    (rule: HighlightingRule) => dispatch(removeHighlightingRule(rule)).then(() => {}),
    [dispatch],
  );

  const description = t('sidebar_highlighting.description', { productName });

  return (
    <HighlightingRules
      description={description}
      onDeleteRule={onDeleteRule}
      onUpdateRules={onUpdateRules}
      onCreateRule={onCreateRule}
      onUpdateRule={onUpdateRule}
    />
  );
};

export default ViewsHighlightingRules;
