// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';
import { type ThemeInterface } from 'theme';

import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';

import { ColorPickerPopover, Icon } from 'components/common';
import ColorPreview from './ColorPreview';

export const HighlightingRuleGrid: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: grid;
  display: -ms-grid;
  margin-top: 5px;
  grid-template-columns: max-content 1fr max-content;
  -ms-grid-columns: max-content 1fr max-content;

  > *:nth-child(1) {
    grid-column: 1;
    -ms-grid-column: 1;
  }
  > *:nth-child(2) {
    grid-column: 2;
    -ms-grid-column: 2;
  }
  > *:nth-child(3) {
    grid-column: 3;
    -ms-grid-column: 3;
  }
`;

const DeleteIcon: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => `
  width: 2rem;
  height: 2rem;
  margin-left: 0.4rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  :active {
    background-color: ${theme.color.gray[90]}
  }
`);

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
    <HighlightingRuleGrid>
      <ColorPickerPopover id="formatting-rule-color"
                          placement="right"
                          color={color}
                          colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                          triggerNode={<ColorPreview color={color} />}
                          onChange={(newColor, _, hidePopover) => updateColor(rule, newColor, hidePopover)} />
      <div>
        for <strong>{field}</strong> = <i>&quot;{value}&quot;</i>.
      </div>
      <DeleteIcon role="presentation" title="Remove this Highlighting Rule" onClick={(e) => onDelete(e, rule)}>
        <Icon name="trash-o" />
      </DeleteIcon>
    </HighlightingRuleGrid>
  );
};

HighlightingRule.propTypes = {
  rule: PropTypes.instanceOf(Rule).isRequired,
};

export default HighlightingRule;
