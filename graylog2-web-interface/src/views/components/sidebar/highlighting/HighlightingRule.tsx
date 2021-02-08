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
import { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { ColorPickerPopover, IconButton } from 'components/common';
import HighlightForm from 'views/components/sidebar/highlighting/HighlightForm';

import ColorPreview from './ColorPreview';

export const HighlightingRuleGrid = styled.div`
  display: grid;
  display: -ms-grid;
  grid-template-columns: max-content 1fr max-content;
  -ms-grid-columns: max-content 1fr max-content;
  margin-top: 10px;
  word-break: break-word;

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

const ButtonContainer = styled.div`
  display: flex;
`;

export const RuleContainer = styled.div`
  padding-top: 4px;
`;

type Props = {
  rule: Rule,
};

const updateColor = (rule, newColor, hidePopover) => {
  const newRule = rule.toBuilder().color(newColor).build();

  return HighlightingRulesActions.update(newRule).then(hidePopover);
};

const onDelete = (rule) => {
  // eslint-disable-next-line no-alert
  if (window.confirm('Do you really want to remove this highlighting?')) {
    HighlightingRulesActions.remove(rule);
  }
};

const HighlightingRule = ({ rule }: Props) => {
  const { field, value, color, condition } = rule;
  const [showForm, setShowForm] = useState(false);

  return (
    <>
      <HighlightingRuleGrid>
        <ColorPickerPopover id="formatting-rule-color"
                            placement="right"
                            color={color}
                            colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                            triggerNode={<ColorPreview color={color} />}
                            onChange={(newColor, _, hidePopover) => updateColor(rule, newColor, hidePopover)} />
        <RuleContainer>
          <strong>{field}</strong> {condition} <i>&quot;{value}&quot;</i>.
        </RuleContainer>
        <ButtonContainer>
          <IconButton title="Edit this Highlighting Rule" name="edit" onClick={() => setShowForm(true)} />
          <IconButton title="Remove this Highlighting Rule" name="trash" onClick={() => onDelete(rule)} />
        </ButtonContainer>
      </HighlightingRuleGrid>
      { showForm && <HighlightForm onClose={() => setShowForm(false)} rule={rule} />}
    </>
  );
};

HighlightingRule.propTypes = {
  rule: PropTypes.instanceOf(Rule).isRequired,
};

export default HighlightingRule;
