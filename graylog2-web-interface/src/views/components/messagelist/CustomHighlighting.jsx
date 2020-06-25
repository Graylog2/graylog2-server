// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import HighlightingRulesContext from 'views/components/contexts/HighlightingRulesContext';

import PossiblyHighlight from './PossiblyHighlight';
import Highlight from './Highlight';

type Props = {
  children: ?React.Node,
  field: string,
  value?: any,
};

const CustomHighlighting = ({ children, field: fieldName, value: fieldValue }: Props) => {
  const decorators = [];
  const highlightingRules = useContext(HighlightingRulesContext) ?? [];

  const highlightingRulesMap = highlightingRules.reduce((prev, cur) => ({ ...prev, [cur.field]: prev[cur.field] ? [...prev[cur.field], cur] : [cur] }), {});
  const rules = highlightingRulesMap[fieldName] ?? [];

  rules.forEach((rule) => {
    const ranges = [];

    if (String(fieldValue) === String(rule.value)) {
      ranges.push({
        start: String(fieldValue).indexOf(rule.value),
        length: String(rule.value).length,
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
  children: PropTypes.node,
  field: PropTypes.string.isRequired,
  value: PropTypes.any,
};

CustomHighlighting.defaultProps = {
  children: undefined,
  value: undefined,
};

export default CustomHighlighting;
