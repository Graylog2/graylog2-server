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
import asMock from 'helpers/mocking/AsMock';

import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewStore } from 'views/stores/ViewStore';

import NewQueryActionHandler from './NewQueryActionHandler';
import View from './views/View';

jest.mock('views/stores/ViewStore', () => ({
  ViewStore: {
    getInitialState: jest.fn(),
  },
}));

jest.mock('views/stores/QueriesStore', () => ({
  QueriesActions: {
    create: jest.fn(() => Promise.resolve()),
  },
}));

describe('NewQueryActionHandler', () => {
  beforeEach(() => {
    asMock(ViewStore.getInitialState).mockReturnValue({
      view: View.create().toBuilder().type(View.Type.Dashboard).build(),
    });
  });

  it('creates a new query', () => NewQueryActionHandler()
    .then(() => expect(QueriesActions.create).toHaveBeenCalled()));
});
