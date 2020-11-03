// @flow strict
import Reflux from 'reflux';

import type { RefluxActions } from 'stores/StoreTypes';
import HTTPHeaderAuthConfig, { type HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';
import { singletonActions } from 'views/logic/singleton';

export type ActionsType = {
  load: () => Promise<HTTPHeaderAuthConfig>,
  update: (payload: HTTPHeaderAuthConfigJSON) => Promise<HTTPHeaderAuthConfig>,
};

type HTTPHeaderAuthConfigActionsType = RefluxActions<ActionsType>;

const HTTPHeaderAuthConfigActions: HTTPHeaderAuthConfigActionsType = singletonActions(
  'HTTPHeaderAuthConfig',
  () => Reflux.createActions({
    load: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

export default HTTPHeaderAuthConfigActions;
