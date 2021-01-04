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
import uuid from 'uuid/v4';

import type { FieldName, FieldValue } from 'views/logic/fieldtypes/FieldType';
import type { ActionContexts } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import FieldType from 'views/logic/fieldtypes/FieldType';

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

export type ActionHandlerArguments = {
  queryId: QueryId,
  field: FieldName,
  value?: FieldValue,
  type: FieldType,
  contexts: ActionContexts,
};

export type ActionHandler = (args: ActionHandlerArguments) => Promise<unknown>;
export type ActionHandlerCondition = (args: ActionHandlerArguments) => boolean;

export type ActionHandlerConditions = {
  isEnabled?: ActionHandlerCondition,
  isHidden?: ActionHandlerCondition,
};

export type HandlerAction = {
  type: string,
  title: string,
  component?: ActionComponentType,
  handler?: ActionHandler,
  resetFocus: boolean,
};

export type ActionDefinition = HandlerAction & ActionHandlerConditions;

// eslint-disable-next-line import/prefer-default-export
export function createHandlerFor(action: ActionDefinition, setActionComponents: SetActionComponents): ActionHandler {
  if (action.handler) {
    return action.handler;
  }

  if (action.component) {
    const ActionComponent = action.component;

    return ({ queryId, field, value, type }) => {
      const id = uuid();
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
