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
import { forwardRef, useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import type Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { ConditionLabelMap } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { ColorPickerPopover, Icon, IconButton } from 'components/common';
import HighlightForm from 'views/components/sidebar/highlighting/HighlightForm';
import type HighlightingColor from 'views/logic/views/formatting/highlighting/HighlightingColor';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import { updateHighlightingRule, removeHighlightingRule } from 'views/logic/slices/highlightActions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { DraggableProps, DragHandleProps } from 'components/common/SortableList';

import ColorPreview from './ColorPreview';

export const Container = styled.div<{ $displayBorder?: boolean }>(({ theme, $displayBorder = true }) => css`
  display: flex;
  padding-top: 5px;
  padding-bottom: 5px;
  word-break: break-word;

  &:not(:last-child) {
    border-bottom: ${$displayBorder ? `1px solid ${theme.colors.global.background}` : 'none'};
  }
`);

const RightCol = styled.div`
  flex: 1;
`;

const ButtonContainer = styled.div`
  display: inline-flex;
  float: right;
`;

export const RuleContainer = styled.div`
  padding-top: 2px;
  display: inline-block;
`;

const DragHandle = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 25px;
  width: 25px;
`;

const updateColor = (rule: Rule, newColor: HighlightingColor, hidePopover: () => void) => async (dispatch: AppDispatch) => dispatch(updateHighlightingRule(rule, { color: newColor })).then(hidePopover);

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

type Props = {
  rule: Rule,
  className?: string,
  draggableProps?: DraggableProps;
  dragHandleProps?: DragHandleProps;
};

const HighlightingRule = forwardRef<HTMLDivElement, Props>(({
  rule,
  className,
  draggableProps,
  dragHandleProps,
}, ref) => {
  const { field, value, color, condition } = rule;
  const [showForm, setShowForm] = useState(false);
  const dispatch = useAppDispatch();
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();

  const _onChange = useCallback((newColor: HighlightingColor, hidePopover: () => void) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_SIDEBAR_HIGHLIGHT_UPDATED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'search-sidebar-highlight-color-update',
    });

    return dispatch(updateColor(rule, newColor, hidePopover));
  }, [dispatch, location.pathname, rule, sendTelemetry]);
  const _onDelete = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_SIDEBAR_HIGHLIGHT_DELETED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_action_value: 'search-sidebar-highlight-delete',
    });

    return dispatch(onDelete(rule));
  }, [dispatch, location.pathname, rule, sendTelemetry]);

  return (
    <Container className={className} ref={ref} {...(draggableProps ?? {})}>
      <RuleColorPreview color={color} onChange={_onChange} />
      <RightCol>
        <RuleContainer data-testid="highlighting-rule">
          <strong>{field}</strong> {ConditionLabelMap[condition]} <i>&quot;{String(value)}&quot;</i>.
        </RuleContainer>
        <ButtonContainer>
          <IconButton title="Edit this Highlighting Rule" name="edit_square" onClick={() => setShowForm(true)} />
          <IconButton title="Remove this Highlighting Rule" name="delete" onClick={_onDelete} />
          {dragHandleProps && (
            <DragHandle {...dragHandleProps}>
              <Icon name="drag_indicator" />
            </DragHandle>
          )}
        </ButtonContainer>
      </RightCol>
      {showForm && <HighlightForm onClose={() => setShowForm(false)} rule={rule} />}
    </Container>
  );
});

export default HighlightingRule;
