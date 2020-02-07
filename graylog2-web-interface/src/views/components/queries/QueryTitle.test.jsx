// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import mockAction from 'helpers/mocking/MockAction';
import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewActions } from 'views/stores/ViewStore';
import Query from 'views/logic/queries/Query';
import QueryTitle from './QueryTitle';

jest.mock('views/stores/QueriesStore', () => ({ QueriesActions: {} }));
jest.mock('views/stores/ViewStore', () => ({ ViewActions: {} }));

describe('QueryTitle', () => {
  beforeEach(() => {
    QueriesActions.duplicate = mockAction(jest.fn(() => Promise.resolve(Query.builder().newId().build())));
    ViewActions.selectQuery = mockAction(jest.fn(queryId => Promise.resolve(queryId)));
  });

  const findAction = (wrapper, name) => {
    const openMenuTrigger = wrapper.find('i[data-testid="query-action-dropdown"]');
    openMenuTrigger.simulate('click');

    wrapper.update();
    const { onSelect } = wrapper.find(`MenuItem[children="${name}"]`).props();
    return () => new Promise((resolve) => {
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
                    onChange={() => {}}
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
                    onChange={() => {}}
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
                    onChange={() => {}}
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
