// @flow strict
import * as Reflux from 'reflux';
import { singletonActions } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

type CustomizationActionsType = RefluxActions<{
  update: (type: string, config: {}) => Promise<mixed>,
  get: (type: string) => Promise<mixed>,
}>;

const CustomizationActions: CustomizationActionsType = singletonActions('customization.actions', () => Reflux.createActions({
  update: { asyncResult: true },
  get: { asyncResult: true },
}));

export default CustomizationActions;
