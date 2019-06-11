// @flow strict
import type { ActionContexts } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';

export type ValuePath = Array<{[string]: any}>;
export type ValueActionHandler = (string, string, any, FieldType) => Promise<*>;
export type ValueActionHandlerWithContext = (string, string, any, FieldType, ActionContexts) => Promise<*>;

export type ValueActionHandlerConditionProps = {
  context: ActionContexts,
  field: string,
  type: FieldType,
  value: any,
};

export type ValueActionHandlerCondition = (ValueActionHandlerConditionProps) => boolean;

