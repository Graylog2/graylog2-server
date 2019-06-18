// @flow strict
import FieldType from '../fieldtypes/FieldType';
import type { ActionContexts } from '../ActionContext';

export type FieldActionHandler = (string, string, FieldType, ActionContexts) => Promise<*>;

export type FieldActionHandlerConditionProps = {
  context: ActionContexts,
  name: string,
  type: FieldType,
};

export type FieldActionHandlerCondition = (FieldActionHandlerConditionProps) => boolean;
