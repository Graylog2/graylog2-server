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
import { useContext } from 'react';
import PropTypes from 'prop-types';

import AppConfig from 'util/AppConfig';
import DateTime from 'logic/datetimes/DateTime';
import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { formatDateTime } from 'components/common/Timestamp';
import FieldType from 'views/logic/fieldtypes/FieldType';

import PossiblyHighlight from './PossiblyHighlight';
import Highlight from './Highlight';

type Props = {
  children?: React.ReactElement,
  field: string,
  value?: any,
};

const CustomHighlighting = ({ children, field: fieldName, value: fieldValue }: Props) => {
  const decorators = [];
  const highlightingRules = useContext(HighlightingRulesContext) ?? [];
  const currentUser = useContext(CurrentUserContext);
  const timezone = currentUser?.timezone ?? AppConfig.rootTimeZone();
  const fieldTypes = useContext(FieldTypesContext);
  let type;

  if (fieldTypes) {
    const { all } = fieldTypes;
    type = (all.find((f) => f.name === fieldName) || { type: FieldType.Unknown }).type.type;
  }

  const highlightingRulesMap = highlightingRules.reduce((prev, cur) => ({ ...prev, [cur.field]: prev[cur.field] ? [...prev[cur.field], cur] : [cur] }), {});
  const rules = highlightingRulesMap[fieldName] ?? [];
  const formattedValue = type === 'date' ? formatDateTime(new DateTime(fieldValue), DateTime.Formats.TIMESTAMP_TZ, timezone) : fieldValue;

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
