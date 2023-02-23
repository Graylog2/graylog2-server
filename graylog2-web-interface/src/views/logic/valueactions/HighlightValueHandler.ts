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
import type { ActionHandlerCondition, ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import type { AppDispatch } from 'stores/useAppDispatch';
import { createHighlightingRule } from 'views/logic/slices/highlightActions';
import type { GetState } from 'views/types';
import { selectHighlightingRules } from 'views/logic/slices/highlightSelectors';

const HighlightValueHandler = ({ field, value }: ActionHandlerArguments) => (dispatch: AppDispatch) => {
  if (value === undefined) {
    return Promise.reject(new Error('Unable to add highlighting for missing value.'));
  }

  return dispatch(createHighlightingRule(field, value));
};

const isEnabled: ActionHandlerCondition<{}> = ({ field, value }, getState: GetState) => {
  const highlightingRules = selectHighlightingRules(getState());

  return highlightingRules.find(({ field: f, value: v }) => (field === f && value === v)) === undefined;
};

HighlightValueHandler.isEnabled = isEnabled;

export default HighlightValueHandler;
