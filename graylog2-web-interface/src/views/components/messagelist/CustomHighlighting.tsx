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
  highlightingRules = [],
}: {
  fieldName: string,
  fieldValue: any,
  formatTime: (fieldValue: DateTime) => string,
  fieldTypes: FieldTypes,
  highlightingRules: Array<HighlightingRule>
}) => {
  let type;

  if (fieldTypes) {
    const { all } = fieldTypes;
    type = (all.find((f) => f.name === fieldName) || { type: FieldType.Unknown }).type.type;
  }

  const rules = highlightingRules.filter((rule) => rule.field === fieldName);
  const formattedValue = type === 'date' ? formatTime(fieldValue) : fieldValue;

  const decorators = rules
    .filter((rule) => rule.conditionFunc(fieldValue, rule.value))
    .map((rule) => {
      const ranges = {
        [fieldName]: [{
          start: 0,
          length: String(formattedValue).length,
        }],
      };

      return ({ field, value }: { field: string, value: any }) => (
        <PossiblyHighlight field={field}
                           value={value}
                           highlightRanges={ranges}
                           color={rule.color} />
      );
    });

  return decorators.length > 0
    ? decorators
    : [Highlight];
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
