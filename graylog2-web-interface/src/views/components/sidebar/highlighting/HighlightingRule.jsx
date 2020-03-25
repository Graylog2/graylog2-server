// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { ColorPickerPopover, Icon } from 'components/common';

import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';

import styles from './HighlightingRules.css';

type Props = {
  rule: Rule,
};

const updateColor = (rule, newColor, hidePopover) => {
  const newRule = rule.toBuilder().color(newColor).build();
  return HighlightingRulesActions.update(newRule).then(hidePopover);
};

const onDelete = (e, rule) => {
  e.preventDefault();
  // eslint-disable-next-line no-alert
  if (window.confirm('Do you really want to remove this highlighting?')) {
    HighlightingRulesActions.remove(rule);
  }
};

const HighlightingRule = ({ rule }: Props) => {
  const { field, value, color } = rule;
  return (
    <div className={styles.highlightingRuleBlock}>
      <ColorPickerPopover id="formatting-rule-color"
                          placement="right"
                          color={color}
                          colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map(c => [c])}
                          triggerNode={<div className={styles.colorElement} style={{ backgroundColor: color }} />}
                          onChange={(newColor, _, hidePopover) => updateColor(rule, newColor, hidePopover)} />
      {' '}
      for <strong>{field}</strong> = <i>&quot;{value}&quot;</i>.
      <span role="presentation" title="Remove this Highlighting Rule" onClick={e => onDelete(e, rule)}>
        <Icon name="trash-o" />
      </span>
    </div>
  );
};

HighlightingRule.propTypes = {
  rule: PropTypes.instanceOf(Rule).isRequired,
};

export default HighlightingRule;
