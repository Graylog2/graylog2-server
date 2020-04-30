// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';

import { DEFAULT_HIGHLIGHT_COLOR } from 'views/Constants';
import { HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';
import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';

import HighlightingRule from './HighlightingRule';
import ColorBox from './ColorBox';
import styles from './HighlightingRules.css';

type Props = {
  rules: Array<Rule>,
};

const HighlightingRules = ({ rules = [] }: Props) => {
  return (
    <>
      <h4 className={styles.headline}>Highlighting</h4>
      <div id="search-term-color" className={styles.highlightingRuleBlock}>
        <ColorBox color={DEFAULT_HIGHLIGHT_COLOR} />
        <span>Search terms</span>
      </div>

      {rules.map((rule) => <HighlightingRule key={`${rule.field}-${rule.value}-${rule.color}`} rule={rule} />)}
    </>
  );
};

HighlightingRules.propTypes = {
  rules: PropTypes.arrayOf(PropTypes.instanceOf(Rule)),
};

HighlightingRules.defaultProps = {
  rules: [],
};

export default connect(HighlightingRules, { rules: HighlightingRulesStore });
