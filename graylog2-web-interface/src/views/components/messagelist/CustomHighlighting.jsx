// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';

import { HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';
import DecoratorContext from 'views/components/messagelist/decoration/DecoratorContext';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

import PossiblyHighlight from './PossiblyHighlight';
import Highlight from './Highlight';

type Props = {
  children: ?React.Node,
  field: string,
  highlightingRules: { [string]: Array<HighlightingRule> },
  value?: any,
};

const CustomHighlighting = ({ children, field: fieldName, value: fieldValue, highlightingRules }: Props) => {
  const decorators = [];
  const rules = highlightingRules[fieldName] || [];
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

export default connect(CustomHighlighting,
  {
    highlightingRules: HighlightingRulesStore,
  },
  ({ highlightingRules }) => {
    const highlightingRulesMap = highlightingRules
      .reduce((prev, cur) => ({ ...prev, [cur.field]: prev[cur.field] ? [...prev[cur.field], cur] : [cur] }), {});
    return { highlightingRules: highlightingRulesMap };
  });
