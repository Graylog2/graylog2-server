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
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';
import mockAction from 'helpers/mocking/MockAction';

import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewActions } from 'views/stores/ViewStore';

import QueryTitle from './QueryTitle';

jest.mock('views/stores/QueriesStore', () => ({ QueriesActions: {} }));
jest.mock('views/stores/ViewStore', () => ({ ViewActions: {} }));

describe('QueryTitle', () => {
  beforeEach(() => {
    QueriesActions.duplicate = mockAction(jest.fn(() => Promise.resolve(Immutable.OrderedMap())));
    ViewActions.selectQuery = mockAction(jest.fn((queryId) => Promise.resolve(queryId)));
  });

  const findAction = (wrapper, name) => {
    const openMenuTrigger = wrapper.find('svg[data-testid="query-action-dropdown"]');

    openMenuTrigger.simulate('click');

    wrapper.update();
    const { onSelect } = wrapper.find(`MenuItem[children="${name}"]`).props();

    return () => new Promise<void>((resolve) => {
      onSelect(undefined, { preventDefault: jest.fn(), stopPropagation: jest.fn() });

      setImmediate(() => {
        resolve();
      });
    });
  };

  describe('duplicate action', () => {
    it('triggers duplication of query', () => {
      const wrapper = mount(
        <QueryTitle active
                    id="deadbeef"
                    openEditModal={() => {}}
                    onClose={() => Promise.resolve()}
                    title="Foo" />,
      );
      const duplicate = findAction(wrapper, 'Duplicate');

      return duplicate().then(() => {
        expect(QueriesActions.duplicate).toHaveBeenCalled();
      });
    });

    it('does not explicitly select new query after duplicating it', () => {
      // Selecting the new query after duplication has become unnecessary, as `ViewStore#createQuery` does it already
      const wrapper = mount(
        <QueryTitle active
                    id="deadbeef"
                    openEditModal={() => {}}
                    onClose={() => Promise.resolve()}
                    title="Foo" />,
      );
      const duplicate = findAction(wrapper, 'Duplicate');

      return duplicate().then(() => {
        expect(ViewActions.selectQuery).not.toHaveBeenCalled();
      });
    });
  });

  describe('edit title action', () => {
    it('opens edit modal', () => {
      const openEditModalFn = jest.fn();
      const wrapper = mount(
        <QueryTitle active
                    id="deadbeef"
                    openEditModal={openEditModalFn}
                    onClose={() => Promise.resolve()}
                    title="Foo" />,
      );
      const clickOnEditOption = findAction(wrapper, 'Edit Title');

      return clickOnEditOption().then(() => {
        expect(openEditModalFn).toHaveBeenCalledTimes(1);
      });
    });
  });
});
