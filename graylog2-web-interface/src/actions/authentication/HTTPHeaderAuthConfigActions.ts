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
import Reflux from 'reflux';

import { RefluxActions } from 'stores/StoreTypes';
import { singletonActions } from 'views/logic/singleton';
import HTTPHeaderAuthConfig, { HTTPHeaderAuthConfigJSON } from 'logic/authentication/HTTPHeaderAuthConfig';

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
