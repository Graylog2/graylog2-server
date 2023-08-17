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
import type { Condition } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import HighlightingRule, { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { selectActiveViewState, selectActiveQuery } from 'views/logic/slices/viewSelectors';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import { updateViewState } from 'views/logic/slices/viewSlice';
import { selectHighlightingRules } from 'views/logic/slices/highlightSelectors';
import type { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

export const updateHighlightingRules = (rules: Array<HighlightingRule>) => async (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const currentViewState = selectActiveViewState(getState());
  const { formatting = FormattingSettings.empty() } = currentViewState;
  const newViewState = currentViewState.toBuilder()
    .formatting(formatting.toBuilder().highlighting(rules).build())
    .build();

  return dispatch(updateViewState(activeQuery, newViewState));
};

export const updateHighlightingRule = (rule: HighlightingRule, payload: Partial<HighlightingRule>) => async (dispatch: AppDispatch, getState: GetState) => {
  const highlightingRules = selectHighlightingRules(getState());

  if (Object.entries(payload).length === 0) {
    return Promise.resolve();
  }

  const newRuleBuilder = Object.entries(payload)
    .reduce((builder, [key, value]) => builder[key](value), rule.toBuilder());
  const newRule = newRuleBuilder.build();

  const newHighlightingRules = highlightingRules.map((currentRule) => (currentRule === rule ? newRule : currentRule));

  return dispatch(updateHighlightingRules(newHighlightingRules));
};

export const addHighlightingRule = (rule: HighlightingRule) => async (dispatch: AppDispatch, getState: GetState) => {
  const activeViewState = selectActiveViewState(getState());
  const { formatting = FormattingSettings.empty() } = activeViewState;

  return dispatch(updateHighlightingRules([...formatting.highlighting, rule]));
};

export const addHighlightingRules = (rules: Array<HighlightingRule>) => async (dispatch: AppDispatch, getState: GetState) => {
  const activeViewState = selectActiveViewState(getState());
  const { formatting = FormattingSettings.empty() } = activeViewState;

  return dispatch(updateHighlightingRules([...formatting.highlighting, ...rules]));
};

export const createHighlightingRule = (field: string, value: any) => async (dispatch: AppDispatch) => {
  const newRule = HighlightingRule.builder()
    .field(field)
    .value(value)
    .color(randomColor())
    .build();

  return dispatch(addHighlightingRule(newRule));
};

export const createHighlightingRules = (rules: Array<{field: string, value: any, condition?: Condition, color?: StaticColor}>) => async (dispatch: AppDispatch) => {
  const newRules = rules.map(({ field, value, color, condition }) => HighlightingRule.builder()
    .field(field)
    .value(value)
    .condition(condition || 'equal')
    .color(color || randomColor())
    .build());

  return dispatch(addHighlightingRules(newRules));
};

export const removeHighlightingRule = (rule: HighlightingRule) => async (dispatch: AppDispatch, getState: GetState) => {
  const highlightingRules = selectHighlightingRules(getState());

  const newHighlightingRules = highlightingRules.filter((r) => r !== rule);

  return dispatch(updateHighlightingRules(newHighlightingRules));
};
