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

import { singleton } from 'logic/singleton';
import type { QueryId } from 'views/logic/queries/Query';
import type { GetState } from 'views/types';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';

import type {
  ActionDefinition,
  ActionHandlerArguments,
  ActionHandlerCondition,
  ExecuteThunkAction,
} from './ActionHandler';

const defaultGetState: GetState = () => ({}) as ReturnType<GetState>;

export type EvaluateActionCondition = <Contexts = unknown>(
  condition: ActionHandlerCondition<Contexts> | undefined,
  args: ActionHandlerArguments<Contexts>,
  fallbackValue: boolean,
) => boolean;

const defaultEvaluateCondition: EvaluateActionCondition = (condition, args, fallbackValue) => {
  if (!condition) {
    return fallbackValue;
  }

  return condition(args, defaultGetState);
};

const defaultExecuteThunkAction: ExecuteThunkAction = () => Promise.resolve(undefined);

export type FieldActionsContextValue = {
  queryId?: QueryId;
  dispatch?: ViewsDispatch;
  getState: GetState;
  evaluateCondition: EvaluateActionCondition;
  executeThunkAction: ExecuteThunkAction;
  valueActions?: Array<ActionDefinition>;
  fieldActions?: Array<ActionDefinition>;
};

export const DEFAULT_FIELD_ACTIONS_CONTEXT: FieldActionsContextValue = {
  queryId: undefined,
  dispatch: undefined,
  getState: defaultGetState,
  evaluateCondition: defaultEvaluateCondition,
  executeThunkAction: defaultExecuteThunkAction,
  valueActions: [],
  fieldActions: [],
};

const FieldActionsContext = React.createContext<FieldActionsContextValue>(DEFAULT_FIELD_ACTIONS_CONTEXT);

export default singleton('contexts.FieldActionsContext', () => FieldActionsContext);
