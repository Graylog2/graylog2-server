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
import PropTypes from 'prop-types';

import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldType from 'views/logic/fieldtypes/FieldType';
import useUserDateTime from 'hooks/useUserDateTime';
import type { DateTime } from 'util/DateTime';
import type HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

import PossiblyHighlight from './PossiblyHighlight';
import Highlight from './Highlight';

const extractDecorators = ({
  fieldName,
  fieldValue,
  formatTime,
  fieldTypes,
  highlightingRules,
}: {
  fieldName: string,
  fieldValue: any,
  formatTime: (fieldValue: DateTime) => string,
  fieldTypes: FieldTypes,
  highlightingRules: Array<HighlightingRule>
}) => {
  const decorators = [];
  let type;

  if (fieldTypes) {
    const { all } = fieldTypes;
    type = (all.find((f) => f.name === fieldName) || { type: FieldType.Unknown }).type.type;
  }

  const highlightingRulesMap = highlightingRules.reduce((prev, cur) => ({ ...prev, [cur.field]: prev[cur.field] ? [...prev[cur.field], cur] : [cur] }), {});
  const rules = highlightingRulesMap[fieldName] ?? [];
  const formattedValue = type === 'date' ? formatTime(fieldValue) : fieldValue;

  rules.forEach((rule) => {
    const ranges = [];

    if (rule.conditionFunc(fieldValue, rule.value)) {
      ranges.push({
        start: String(formattedValue).indexOf(formattedValue),
        length: String(formattedValue).length,
      });
    }

    if (ranges.length > 0) {
      decorators.push(({ field, value }) => (
        <PossiblyHighlight field={field}
                           value={value}
                           highlightRanges={ranges.length > 0 ? { [fieldName]: ranges } : {}}
                           color={rule.color} />
      ));
    }
  });

  if (decorators.length === 0) {
    decorators.push(Highlight);
  }

  return decorators;
};

type Props = {
  children?: React.ReactElement,
  field: string,
  value?: any,
};

const CustomHighlighting = ({ children, field: fieldName, value: fieldValue }: Props) => {
  const { formatTime } = useUserDateTime();
  const highlightingRules = useContext(HighlightingRulesContext);
  const fieldTypes = useContext(FieldTypesContext);

  const decorators = useMemo(() => extractDecorators(({
    fieldName,
    fieldValue,
    formatTime,
    fieldTypes,
    highlightingRules,
  })), [
    fieldName,
    fieldValue,
    formatTime,
    fieldTypes,
    highlightingRules,
  ]);

  return (
    <DecoratorContext.Provider value={decorators}>
      {children}
    </DecoratorContext.Provider>
  );
};

CustomHighlighting.propTypes = {
  children: PropTypes.element,
  field: PropTypes.string.isRequired,
  value: PropTypes.any,
};

CustomHighlighting.defaultProps = {
  children: undefined,
  value: undefined,
};

export default CustomHighlighting;
