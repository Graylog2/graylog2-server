// @flow strict
import { ActionContext } from '../ActionContext';
import FieldType from '../fieldtypes/FieldType';

export type ValueActionHandler = (string, string, any, FieldType) => Promise<*>;
export type ValueActionHandlerWithContext = (string, string, any, FieldType, ActionContext) => Promise<*>;
