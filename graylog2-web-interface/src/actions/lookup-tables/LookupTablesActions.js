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

import { singletonActions } from 'views/logic/singleton';

const LookupTablesActions = singletonActions('LookupTables', () => Reflux.createActions({
  searchPaginated: { asyncResult: true },
  reloadPage: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  update: { asyncResult: true },
  getErrors: { asyncResult: true },
  lookup: { asyncResult: true },
  purgeKey: { asyncResult: true },
  purgeAll: { asyncResult: true },
  validate: { asyncResult: true },
}));

export default LookupTablesActions;
