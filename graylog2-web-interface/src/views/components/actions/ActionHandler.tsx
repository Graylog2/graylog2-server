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

import type { ActionContexts, GetState } from 'views/types';
import type { FieldName, FieldValue } from 'views/logic/fieldtypes/FieldType';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import generateId from 'logic/generateId';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';

export type ActionHandlerArguments = {
  field: FieldName;
  value?: FieldValue;
  type: FieldType;
  contexts: Partial<ActionContexts>;
};

export type ResolvedActionHandlerArguments<T extends object = object> = ActionHandlerArguments & T;

export type ActionComponentProps<T extends object = object> = {
  onClose: () => void;
  handlerArgs: ResolvedActionHandlerArguments<T>;
};

export type ActionComponentType<T extends object = object> = React.ComponentType<ActionComponentProps<T>>;

export type ActionComponents = {
  [key: string]: React.ReactElement<ActionComponentProps>;
};

export type SetActionComponents = (fn: (component: ActionComponents) => ActionComponents) => void;

export type ActionHandler<T extends object = object> = (args: ResolvedActionHandlerArguments<T>) => Promise<unknown>;
export type ActionHandlerCondition<T extends object = object> = (
  args: ResolvedActionHandlerArguments<T>,
  getState: GetState,
) => boolean;

export type ActionConditions<T extends object = object> = {
  isEnabled?: ActionHandlerCondition<T>;
  isHidden?: ActionHandlerCondition<T>;
};

type ActionDefinitionBase<T extends object = object> = {
  type: string;
  title: string;
  resetFocus?: boolean;
  help?: (args: ResolvedActionHandlerArguments<T>) => { title: string; description: React.ReactNode } | undefined;
  condition?: () => boolean;
};

export type ThunkActionHandler<T extends object = object> = (
  args: ResolvedActionHandlerArguments<T>,
) => (dispatch: ViewsDispatch, getState: GetState) => unknown | Promise<unknown>;

export type ExecuteThunkAction = <T extends object>(
  thunk: ThunkActionHandler<T>,
  args: ResolvedActionHandlerArguments<T>,
) => Promise<unknown>;

type FunctionHandlerAction<T extends object = object> = {
  handler: ActionHandler<T>;
};
type ThunkHandlerAction<T extends object = object> = {
  thunk: ThunkActionHandler<T>;
};
type ComponentsHandlerAction<T extends object = object> = {
  component: ActionComponentType<T>;
};

export type HandlerAction<T extends object = object> = (
  | FunctionHandlerAction<T>
  | ComponentsHandlerAction<T>
  | ThunkHandlerAction<T>
) &
  ActionDefinitionBase<T>;

export type ExternalLinkAction<T extends object = object> = {
  linkTarget: (args: ResolvedActionHandlerArguments<T>) => string;
} & ActionDefinitionBase<T>;

export type ActionDefinition<T extends object = object> = (HandlerAction<T> | ExternalLinkAction<T>) &
  ActionConditions<T>;

export function isExternalLinkAction<T extends object>(action: ActionDefinition<T>): action is ExternalLinkAction<T> {
  return 'linkTarget' in action;
}

export function createHandlerFor<T extends object = object>(
  executeThunkAction: ExecuteThunkAction,
  action: ActionDefinitionBase<T> & HandlerAction<T>,
  setActionComponents: SetActionComponents,
): ActionHandler<T> {
  if ('handler' in action) {
    return action.handler;
  }

  if ('thunk' in action) {
    return async (args: ResolvedActionHandlerArguments<T>) => executeThunkAction(action.thunk, args);
  }

  if (action.component) {
    const ActionComponent = action.component;

    return (args: ResolvedActionHandlerArguments<T>) => {
      const id = generateId();
      const onClose = () => setActionComponents(({ [id]: _, ...rest }) => rest);
      const commonProps = {
        key: action.title,
        onClose,
        handlerArgs: args,
      };

      const renderedComponent = <ActionComponent {...commonProps} />;

      setActionComponents(
        (actionComponents) =>
          ({
            [id]: renderedComponent,
            ...actionComponents,
          }) as ActionComponents,
      );

      return Promise.resolve();
    };
  }

  throw new Error(`Invalid binding for action: ${String(action)} - has neither 'handler' nor 'component'.`);
}
