// @flow strict
import * as React from 'react';
import uuid from 'uuid/v4';
import type { FieldName, FieldValue } from 'views/logic/fieldtypes/FieldType';
import type { ActionContexts } from 'views/logic/ActionContext';
import type { QueryId } from 'views/logic/queries/Query';
import FieldType from 'views/logic/fieldtypes/FieldType';

export type ActionComponents = { [string]: React.Node };

export type SetActionComponents = ((ActionComponents) => ActionComponents) => void;

export type ActionHandlerArguments = {|
  queryId: QueryId,
  field: FieldName,
  value?: FieldValue,
  type: FieldType,
  contexts: ActionContexts
|};

export type ActionHandler = (ActionHandlerArguments) => Promise<mixed>;
export type ActionHandlerCondition = (ActionHandlerArguments) => boolean;

export type ActionHandlerConditions = {
  isEnabled?: ActionHandlerCondition,
  isHidden?: ActionHandlerCondition,
};

export type ActionComponentProps = {
  onClose: () => void,
  queryId: QueryId,
  field: FieldName,
  type: FieldType,
  value: ?FieldValue,
};

export type ActionComponentType = React.AbstractComponent<ActionComponentProps>;
export type HandlerAction = {|
  type: string,
  title: string,
  component?: ActionComponentType,
  handler?: ActionHandler,
|};

export type ActionDefinition = HandlerAction & ActionHandlerConditions;

// eslint-disable-next-line import/prefer-default-export
export function createHandlerFor(action: ActionDefinition, setActionComponents: SetActionComponents): ActionHandler {
  if (action.handler) {
    return action.handler;
  }
  if (action.component) {
    const ActionComponent = action.component;
    // eslint-disable-next-line no-unused-vars
    return ({ queryId, field, value, type }) => {
      const id = uuid();
      const onClose = () => setActionComponents(({ [id]: _, ...rest }) => rest);
      const renderedComponent = (
        <ActionComponent key={action.title}
                         onClose={onClose}
                         queryId={queryId}
                         field={field}
                         value={value}
                         type={type} />
      );
      setActionComponents((actionComponents) => ({ [id]: renderedComponent, ...actionComponents }));
      return Promise.resolve();
    };
  }
  throw new Error(`Invalid binding for action: ${String(action)} - has neither 'handler' nor 'component'.`);
}
