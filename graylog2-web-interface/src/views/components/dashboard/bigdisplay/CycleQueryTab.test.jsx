// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import asMock from 'helpers/mocking/AsMock';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import { ViewActions } from 'views/stores/ViewStore';

import CycleQueryTab from './CycleQueryTab';

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    selectQuery: jest.fn(),
  },
}));

const search = Search.create().toBuilder().queries([
  Query.builder().id('foo').build(),
  Query.builder().id('bar').build(),
  Query.builder().id('baz').build(),
]).build();
const view = View.create().toBuilder().search(search).build();

describe('CycleQueryTab', () => {
  describe('cycles tabs:', () => {
    let wrapper;
    afterEach(() => {
      wrapper.unmount();
    });
    it('does not return markup', () => {
      wrapper = mount(<CycleQueryTab view={view} activeQuery="bar" interval={1} tabs={[1, 2]} />);
      expect(wrapper).toBeEmptyRender();
    });
    it('should switch to next tab after interval', () => {
      return new Promise((resolve) => {
        asMock(ViewActions.selectQuery).mockImplementationOnce((queryId) => {
          expect(queryId).toEqual('baz');
          resolve();
        });

        wrapper = mount(<CycleQueryTab view={view} activeQuery="bar" interval={1} tabs={[1, 2]} />);
      });
    });
    it('should switch to first tab if current one is the last', () => {
      return new Promise((resolve) => {
        asMock(ViewActions.selectQuery).mockImplementationOnce((queryId) => {
          expect(queryId).toEqual('foo');
          resolve();
        });

        wrapper = mount(<CycleQueryTab view={view} activeQuery="baz" interval={1} tabs={[0, 1, 2]} />);
      });
    });
    it('should switch to next tab skipping gaps after interval', () => {
      return new Promise((resolve) => {
        asMock(ViewActions.selectQuery).mockImplementationOnce((queryId) => {
          expect(queryId).toEqual('baz');
          resolve();
        });

        wrapper = mount(<CycleQueryTab view={view} activeQuery="foo" interval={1} tabs={[0, 2]} />);
      });
    });
    it('should switch to next tab defaulting to all tabs if `tabs` prop` is left out', () => {
      return new Promise((resolve) => {
        asMock(ViewActions.selectQuery).mockImplementationOnce((queryId) => {
          expect(queryId).toEqual('bar');
          resolve();
        });

        wrapper = mount(<CycleQueryTab view={view} activeQuery="foo" tabs={[1]} interval={1} />);
      });
    });
  });

  describe('uses setInterval/clearInterval properly', () => {
    const origSetInterval = window.setInterval;
    const origClearInterval = window.clearInterval;
    beforeEach(() => {
      jest.resetAllMocks();
      window.setInterval = jest.fn(() => 'deadbeef');
      window.clearInterval = jest.fn();
    });
    afterAll(() => {
      window.setInterval = origSetInterval;
      window.clearInterval = origClearInterval;
    });
    it('passes the correct interval to setInterval', () => {
      const wrapper = mount(<CycleQueryTab view={view} activeQuery="foo" interval={42} />);
      expect(window.setInterval).toHaveBeenCalledTimes(1);
      expect(window.setInterval).toHaveBeenCalledWith(expect.anything(), 42000);
      wrapper.unmount();
      expect(ViewActions.selectQuery).not.toHaveBeenCalled();
    });
    it('when unmounting', () => {
      const wrapper = mount(<CycleQueryTab view={view} activeQuery="foo" interval={1} />);
      wrapper.unmount();
      expect(window.clearInterval).toHaveBeenCalledWith('deadbeef');
    });
  });
});
