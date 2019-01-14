// @flow strict
import { ActionContext } from '../ActionContext';

export type ValueActionHandler = (string, string, string) => void;
export type ValueActionHandlerWithContext = (string, string, string, ActionContext) => void;
