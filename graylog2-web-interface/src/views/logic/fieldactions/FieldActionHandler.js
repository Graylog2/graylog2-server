// @flow strict
import FieldType from '../fieldtypes/FieldType';
import type { ActionContexts } from '../ActionContext';

export type FieldActionHandlerConditionProps = {
  context: ActionContexts,
  name: string,
  type: FieldType,
};

export type FieldActionHandlerCondition = (FieldActionHandlerConditionProps) => boolean;

type Conditions = {
  condition?: FieldActionHandlerCondition,
  hide?: FieldActionHandlerCondition,
}
export type FieldActionHandler = ((string, string, FieldType, ActionContexts) => Promise<*>) & Conditions;
