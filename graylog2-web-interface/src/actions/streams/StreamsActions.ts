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

import type { RefluxActions } from 'stores/StoreTypes';
import { singletonActions } from 'logic/singleton';

export type ActionsType = {
  searchPaginated: (...args: Array<unknown>) => Promise<unknown>;
  listStreams: (...args: Array<unknown>) => Promise<unknown>;
  load: (...args: Array<unknown>) => Promise<unknown>;
  get: (...args: Array<unknown>) => Promise<unknown>;
  remove: (...args: Array<unknown>) => Promise<unknown>;
  pause: (...args: Array<unknown>) => Promise<unknown>;
  resume: (...args: Array<unknown>) => Promise<unknown>;
  cloneStream: (...args: Array<unknown>) => Promise<unknown>;
  update: (...args: Array<unknown>) => Promise<unknown>;
  save: (...args: Array<unknown>) => Promise<unknown>;
  removeOutput: (...args: Array<unknown>) => Promise<unknown>;
  addOutput: (...args: Array<unknown>) => Promise<unknown>;
  testMatch: (...args: Array<unknown>) => Promise<unknown>;
};

type StreamsActionsType = RefluxActions<ActionsType>;

const StreamsActions: StreamsActionsType = singletonActions('Streams', () =>
  Reflux.createActions({
    searchPaginated: { asyncResult: true },
    listStreams: { asyncResult: true },
    load: { asyncResult: true },
    get: { asyncResult: true },
    remove: { asyncResult: true },
    pause: { asyncResult: true },
    resume: { asyncResult: true },
    cloneStream: { asyncResult: true },
    update: { asyncResult: true },
    save: { asyncResult: true },
    removeOutput: { asyncResult: true },
    addOutput: { asyncResult: true },
    testMatch: { asyncResult: true },
  }),
);

export default StreamsActions;
