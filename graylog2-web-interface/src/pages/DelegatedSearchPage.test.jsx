jest.mock('c3', () => ({ default: jest.fn() }))
  // eslint-disable-next-line global-require
  .mock('injection/CombinedProvider', () => new (require('helpers/mocking').CombinedProviderMock)());

/* eslint-disable import/first */
import React from 'react';
import { shallow } from 'wrappedEnzyme';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import DelegatedSearchPage from 'pages/DelegatedSearchPage';
/* eslint-enable import/first */


test('Renders other component if registered', () => {
  const SimpleComponent = () => <div>Hello!</div>;

  PluginStore.register(new PluginManifest({}, {
    pages: {
      search: { component: SimpleComponent },
    },
  }));

  const tree = shallow(<DelegatedSearchPage />);

  expect(tree).toMatchSnapshot();
});
