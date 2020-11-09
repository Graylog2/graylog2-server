// @flow strict
import type { ActionsType } from 'actions/authentication/HTTPHeaderAuthConfigActions';
import { HTTPHeaderAuthConfigActions } from 'stores/authentication/HTTPHeaderAuthConfigStore';

import notifyingAction from '../notifyingAction';

const load: $PropertyType<ActionsType, 'load'> = notifyingAction({
  action: HTTPHeaderAuthConfigActions.load,
  error: (error) => ({
    message: `Loading HTTP header authentication config failed with status: ${error}`,
  }),
});

const update: $PropertyType<ActionsType, 'update'> = notifyingAction({
  action: HTTPHeaderAuthConfigActions.update,
  success: () => ({
    message: 'Successfully updated HTTP header authentication config',
  }),
  error: (error) => ({
    message: `Updating HTTP header authentication config failed with status: ${error}`,
  }),
});

export default {
  load,
  update,
};
