/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import { $PropertyType } from 'utility-types';

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
