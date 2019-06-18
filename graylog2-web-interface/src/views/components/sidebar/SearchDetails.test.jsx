// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';

const searchResult = {
  effectiveTimerange: {
    from: '2018-12-18T14:34:49.519Z',
    to: '2018-12-18T14:39:49.519Z',
  },
};

type CurrentUserStoreType = {
  listen: () => void,
  get: () => any,
  getInitialState: () => { currentUser: { timezone: ?string } },
};

const currentUser = { timezone: 'UTC' };

describe('SearchDetails', () => {
  const CurrentUserStore: CurrentUserStoreType = {
    listen: jest.fn(),
    get: jest.fn(() => currentUser),
    getInitialState: jest.fn(() => ({ currentUser })),
  };

  jest.doMock('injection/CombinedProvider', () => ({
    get: () => ({
      CurrentUserStore,
    }),
  }));

  // eslint-disable-next-line global-require
  const SearchDetails = require('./SearchDetails');

  const updateCurrentUser = (newCurrentUser) => {
    const cb = CurrentUserStore.listen.mock.calls[0][0];
    cb({ currentUser: newCurrentUser });
  };

  beforeEach(() => {
    updateCurrentUser(currentUser);
  });

  it('renders effective timerange in UTC from search result', () => {
    const wrapper = mount(<SearchDetails results={searchResult} />);

    expect(wrapper).toContainReact(<time title="2018-12-18T14:34:49.519Z"
                                         dateTime="2018-12-18T14:34:49.519Z">2018-12-18 14:34:49.519
    </time>);
    expect(wrapper).toContainReact(<time title="2018-12-18T14:39:49.519Z"
                                         dateTime="2018-12-18T14:39:49.519Z">2018-12-18 14:39:49.519
    </time>);
  });

  it('renders effective timerange in Europe/Berlin from search result', () => {
    const timezone = 'Europe/Berlin';
    updateCurrentUser({ timezone });

    const wrapper = mount(<SearchDetails results={searchResult} />);

    expect(wrapper).toContainReact(<time title="2018-12-18T14:34:49.519Z"
                                         dateTime="2018-12-18T14:34:49.519Z">2018-12-18 15:34:49.519
    </time>);
    expect(wrapper).toContainReact(<time title="2018-12-18T14:39:49.519Z"
                                         dateTime="2018-12-18T14:39:49.519Z">2018-12-18 15:39:49.519
    </time>);
  });

  it('renders effective timerange from search result in browser\'s timezone if user timezone is null', () => {
    const timezone = null;
    updateCurrentUser({ timezone });
    // $FlowFixMe: Cheap and dirty Intl API mock
    Intl.DateTimeFormat = () => ({ resolvedOptions: () => ({ timeZone: 'America/Puerto_Rico' }) });

    const wrapper = mount(<SearchDetails results={searchResult} />);

    expect(wrapper).toContainReact(<time title="2018-12-18T14:34:49.519Z"
                                         dateTime="2018-12-18T14:34:49.519Z">2018-12-18 10:34:49.519
    </time>);
    expect(wrapper).toContainReact(<time title="2018-12-18T14:39:49.519Z"
                                         dateTime="2018-12-18T14:39:49.519Z">2018-12-18 10:39:49.519
    </time>);
  });
});
