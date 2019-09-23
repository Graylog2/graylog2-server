// @flow strict
import type { ActionContexts } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';

export type ValuePath = Array<{[string]: any}>;

export type ValueActionHandlerConditionProps = {
  context: ActionContexts,
  field: string,
  type: FieldType,
  value: any,
};
export type ValueActionHandlerCondition = (ValueActionHandlerConditionProps) => boolean;
export type ValueActionHideCondition = (ActionContexts) => boolean;

export type ValueActionHandler = ((string, string, any, FieldType) => Promise<*>) & { condition?: ValueActionHandlerCondition };
export type Conditions = {
  isEnabled?: ValueActionHandlerCondition,
  hide?: ValueActionHideCondition,
}
export type ValueActionHandlerWithContext = ((string, string, any, FieldType, ActionContexts) => Promise<*>) & Conditions;
