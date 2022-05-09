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

import type { ActionContexts } from 'views/types';
import type { FieldName, FieldValue } from 'views/logic/fieldtypes/FieldType';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { QueryId } from 'views/logic/queries/Query';
import generateId from 'logic/generateId';

export type ActionComponentProps = {
  onClose: () => void,
  queryId: QueryId,
  field: FieldName,
  type: FieldType,
  value: FieldValue | undefined | null,
};

export type ActionComponentType = React.ComponentType<ActionComponentProps>;

export type ActionComponents = { [key: string]: React.ReactElement<ActionComponentProps> };

export type SetActionComponents = (fn: (component: ActionComponents) => ActionComponents) => void;

export type ActionHandlerArguments<Contexts = ActionContexts> = {
  queryId: QueryId,
  field: FieldName,
  value?: FieldValue,
  type: FieldType,
  contexts: Contexts,
};

export type ActionHandler<Contexts> = (args: ActionHandlerArguments<Contexts>) => Promise<unknown>;
export type ActionHandlerCondition<Contexts> = (args: ActionHandlerArguments<Contexts>) => boolean;

export type ActionConditions<Contexts> = {
  isEnabled?: ActionHandlerCondition<Contexts>,
  isHidden?: ActionHandlerCondition<Contexts>,
};

type ActionDefinitionBase<Contexts> = {
  type: string,
  title: string,
  resetFocus?: boolean,
  help?: (args: ActionHandlerArguments<Contexts>) => { title: string, description: React.ReactNode } | undefined,
  condition?: () => boolean,
};

type FunctionHandlerAction<Contexts> = {
  handler: ActionHandler<Contexts>,
};
type ComponentsHandlerAction = {
  component: ActionComponentType,
};

export type HandlerAction<Contexts> =
  (FunctionHandlerAction<Contexts> | ComponentsHandlerAction)
  & ActionDefinitionBase<Contexts>;

export type ExternalLinkAction<Contexts> = {
  linkTarget: (args: ActionHandlerArguments<Contexts>) => string,
} & ActionDefinitionBase<Contexts>;

export type ActionDefinition<Contexts = ActionContexts> =
  (HandlerAction<Contexts> | ExternalLinkAction<Contexts>)
  & ActionConditions<Contexts>;

export function isExternalLinkAction<T>(action: ActionDefinition<T>): action is ExternalLinkAction<T> {
  return 'linkTarget' in action;
}

export function createHandlerFor<T>(action: ActionDefinitionBase<T> & HandlerAction<T>, setActionComponents: SetActionComponents): ActionHandler<T> {
  if ('handler' in action) {
    return action.handler;
  }

  if (action.component) {
    const ActionComponent = action.component;

    return ({ queryId, field, value, type }) => {
      const id = generateId();
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const onClose = () => setActionComponents(({ [id]: _, ...rest }) => rest);
      const renderedComponent = (
        <ActionComponent key={action.title}
                         onClose={onClose}
                         queryId={queryId}
                         field={field}
                         value={value}
                         type={type} />
      );

      setActionComponents((actionComponents) => ({ [id]: renderedComponent, ...actionComponents } as ActionComponents));

      return Promise.resolve();
    };
  }

  throw new Error(`Invalid binding for action: ${String(action)} - has neither 'handler' nor 'component'.`);
}
