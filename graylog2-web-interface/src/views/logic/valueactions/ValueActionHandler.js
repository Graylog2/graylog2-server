// @flow strict
import type { ActionHandler, ActionHandlerConditions } from 'views/components/actions/ActionHandler';

export type ValuePath = Array<{[string]: any}>;

export type ValueActionHandler = ActionHandler & ActionHandlerConditions;
