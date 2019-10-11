// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';

import mockAction from 'helpers/mocking/MockAction';
import asMock from 'helpers/mocking/AsMock';
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
  describe('duplicate action', () => {
    const findAction = (wrapper, name) => {
      const openMenuTrigger = wrapper.find('i[className="fa fa-chevron-down"]');
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

    it('triggers duplication of query', () => {
      const wrapper = mount(<QueryTitle active id="deadbeef" onChange={() => {}} onClose={() => {}} value="Foo" />);
      const duplicate = findAction(wrapper, 'Duplicate');

      return duplicate().then(() => {
        expect(QueriesActions.duplicate).toHaveBeenCalled();
      });
    });
    it('selects new query after duplicating it', () => {
      const wrapper = mount(<QueryTitle active id="deadbeef" onChange={() => {}} onClose={() => {}} value="Foo" />);
      const duplicate = findAction(wrapper, 'Duplicate');

      return duplicate().then(() => {
        expect(ViewActions.selectQuery).toHaveBeenCalled();
        expect(asMock(ViewActions.selectQuery).mock.calls[0][0]).not.toBeNull();
      });
    });
  });
  it('double clicking the query title opens input', () => {
    const wrapper = mount(<QueryTitle active id="deadbeef" onChange={() => {}} onClose={() => {}} value="Foo" />);
    expect(wrapper.find('input[value="Foo"]')).not.toExist();
    const title = wrapper.find('span[children="Foo"]');

    title.simulate('doubleClick');
    wrapper.update();

    expect(wrapper.find('input[value="Foo"]')).toExist();
  });
  it('double clicking the query title and changing the name triggers change event', () => {
    const onChange = jest.fn();
    const wrapper = mount(<QueryTitle active id="deadbeef" onChange={onChange} onClose={() => {}} value="Foo" />);
    const title = wrapper.find('span[children="Foo"]');

    title.simulate('doubleClick');
    wrapper.update();

    const input = wrapper.find('input[value="Foo"]');

    input.simulate('change', { target: { value: 'Bar' } });

    const form = wrapper.find('form');
    form.simulate('submit');

    expect(onChange).toHaveBeenCalledWith('Bar');
  });
});
