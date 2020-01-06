// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import mockAction from 'helpers/mocking/MockAction';
import View from 'views/logic/views/View';
import { ViewActions } from 'views/stores/ViewStore';

import StreamSearchPage from './StreamSearchPage';

jest.mock('views/stores/SearchStore', () => ({ SearchActions: {} }));
jest.mock('views/stores/ViewStore', () => ({ ViewActions: {} }));
jest.mock('views/stores/QueryFiltersStore', () => ({ QueryFiltersStoreActions: {} }));

describe('StreamSearchPage', () => {
  const SimpleStreamSearchPage = props => (
    <StreamSearchPage location={{ query: {} }}
                      params={{ streamId: 'stream-id-1' }}
                      route={{}}
                      {...props} />
  );

  beforeEach(() => {
    jest.resetAllMocks();
    jest.resetModules();
  });

  it('created view with streamId passed from props', () => {
    ViewActions.create = mockAction(jest.fn(() => Promise.resolve()));
    mount(<SimpleStreamSearchPage />);
    expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1');
  });

  it('recreated view when streamId passed from props changes', () => {
    ViewActions.create = mockAction(jest.fn(() => Promise.resolve()));
    const wrapper = mount(<SimpleStreamSearchPage />);
    expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-1');

    wrapper.setProps({ params: { streamId: 'stream-id-2' } });

    expect(ViewActions.create).toHaveBeenCalledWith(View.Type.Search, 'stream-id-2');
  });
});
