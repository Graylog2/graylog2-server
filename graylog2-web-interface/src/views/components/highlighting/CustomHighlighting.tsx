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
import { useContext, useMemo } from 'react';

import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import type HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

import Highlight from './Highlight';

const extractDecorators = ({
  fieldName,
  fieldValue,
  highlightingRules = [],
}: {
  fieldName: string,
  fieldValue: any,
  highlightingRules: Array<HighlightingRule>
}) => highlightingRules.filter((rule) => rule.field === fieldName)
  .find((rule) => rule.conditionFunc(fieldValue, rule.value));

type Props = {
  children?: React.ReactElement,
  field: string,
  value?: any,
};

const CustomHighlighting = ({ children, field: fieldName, value: fieldValue }: Props) => {
  const highlightingRules = useContext(HighlightingRulesContext);

  const matchingRule = useMemo(() => extractDecorators(({
    fieldName,
    fieldValue,
    highlightingRules,
  })), [fieldName, fieldValue, highlightingRules]);

  return matchingRule ? <Highlight color={matchingRule.color.colorFor(fieldValue)}>{children}</Highlight> : children;
};

export default CustomHighlighting;
