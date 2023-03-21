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
import { useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import Rule, { ConditionLabelMap } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { ColorPickerPopover, IconButton } from 'components/common';
import HighlightForm from 'views/components/sidebar/highlighting/HighlightForm';
import type HighlightingColor from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import { updateHighlightingRule, removeHighlightingRule } from 'views/logic/slices/highlightActions';

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

const updateColor = (rule: Rule, newColor: HighlightingColor, hidePopover: () => void) => async (dispatch: AppDispatch) => {
  return dispatch(updateHighlightingRule(rule, { color: newColor })).then(hidePopover);
};

const onDelete = (rule: Rule) => async (dispatch: AppDispatch) => {
  // eslint-disable-next-line no-alert
  if (window.confirm('Do you really want to remove this highlighting?')) {
    return dispatch(removeHighlightingRule(rule));
  }

  return Promise.resolve();
};

type RuleColorPreviewProps = {
  color: HighlightingColor,
  onChange: (newColor: HighlightingColor, hidePopover: () => void) => void,
};

const RuleColorPreview = ({ color, onChange }: RuleColorPreviewProps) => {
  const _onChange = useCallback((newColor: string, _ignored: React.ChangeEvent<HTMLInputElement>, hidePopover: () => void) => onChange(StaticColor.create(newColor), hidePopover), [onChange]);

  if (color.isStatic()) {
    return (
      <ColorPickerPopover id="formatting-rule-color"
                          placement="right"
                          color={color.color}
                          colors={DEFAULT_CUSTOM_HIGHLIGHT_RANGE.map((c) => [c])}
                          triggerNode={<ColorPreview color={color} />}
                          onChange={_onChange} />
    );
  }

  if (color.isGradient()) {
    return <ColorPreview color={color} />;
  }

  throw new Error(`Invalid highlighting color: ${color}`);
};

const HighlightingRule = ({ rule }: Props) => {
  const { field, value, color, condition } = rule;
  const [showForm, setShowForm] = useState(false);
  const dispatch = useAppDispatch();

  const _onChange = useCallback((newColor: HighlightingColor, hidePopover: () => void) => dispatch(updateColor(rule, newColor, hidePopover)), [dispatch, rule]);
  const _onDelete = useCallback(() => dispatch(onDelete(rule)), [dispatch, rule]);

  return (
    <>
      <HighlightingRuleGrid>
        <RuleColorPreview color={color} onChange={_onChange} />
        <RuleContainer data-testid="highlighting-rule">
          <strong>{field}</strong> {ConditionLabelMap[condition]} <i>&quot;{String(value)}&quot;</i>.
        </RuleContainer>
        <ButtonContainer>
          <IconButton title="Edit this Highlighting Rule" name="edit" onClick={() => setShowForm(true)} />
          <IconButton title="Remove this Highlighting Rule" name="trash-alt" onClick={_onDelete} />
        </ButtonContainer>
      </HighlightingRuleGrid>
      {showForm && <HighlightForm onClose={() => setShowForm(false)} rule={rule} />}
    </>
  );
};

HighlightingRule.propTypes = {
  rule: PropTypes.instanceOf(Rule).isRequired,
};

export default HighlightingRule;
