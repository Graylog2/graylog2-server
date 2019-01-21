// @flow strict
import { ActionContext } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';

export type FieldActionHandler = (string, string, FieldType) => Promise<*>;
export type FieldActionHandlerWithContext = (string, string, FieldType, ActionContext) => Promise<*>;

export type FieldActionHandlerCondition = ({ context: ActionContext, name: string }) => boolean;
