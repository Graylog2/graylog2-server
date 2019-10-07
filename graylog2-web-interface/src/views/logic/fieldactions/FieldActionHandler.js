// @flow strict
import type {
  ActionHandler,
  ActionHandlerArguments,
  ActionHandlerConditions,
} from 'views/components/actions/ActionHandler';

export type FieldActionHandlerCondition = (ActionHandlerArguments) => boolean;

export type FieldActionHandler = ActionHandler & ActionHandlerConditions;
