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
import { MockStore } from 'helpers/mocking';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

import { ViewManagementActions } from './ViewManagementStore';

jest.mock('stores/users/CurrentUserStore', () => ({ CurrentUserStore: MockStore('reload') }));
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('ViewManagementStore', () => {
  it('refreshes user after creating a view', () => {
    const search = Search.create();
    const view = View.builder().newId().search(search).build();

    return ViewManagementActions.create(view).then(() => {
      expect(CurrentUserStore.reload).toHaveBeenCalled();
    });
  });
});
