// @flow strict
import FieldType from '../fieldtypes/FieldType';
import type { ActionContexts } from '../ActionContext';

export type FieldActionHandler = (string, string, FieldType) => Promise<*>;
export type FieldActionHandlerWithContext = (string, string, FieldType, ActionContexts) => Promise<*>;

export type FieldActionHandlerCondition = ({ context: ActionContexts, name: string }) => boolean;
