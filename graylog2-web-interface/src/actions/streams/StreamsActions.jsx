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

import { singletonActions } from 'logic/singleton';

const StreamsActions = singletonActions('Streams', () => Reflux.createActions({
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
}));

export default StreamsActions;
