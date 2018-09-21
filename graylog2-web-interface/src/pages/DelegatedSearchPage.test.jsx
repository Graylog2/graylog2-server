jest.mock('c3', () => ({ default: jest.fn() }))
  .mock('components/search/LegacyHistogram', () => ({ default: jest.fn() }))
  .mock('components/search/QueryInput', () => ({ default: jest.fn() }))
  // eslint-disable-next-line global-require
  .mock('injection/CombinedProvider', () => new (require('helpers/mocking').CombinedProviderMock)());

/* eslint-disable import/first */
import React from 'react';
import ShallowRenderer from 'react-test-renderer/shallow';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import DelegatedSearchPage from 'pages/DelegatedSearchPage';
/* eslint-enable import/first */


test('Renders SearchPage by default', () => {
  const renderer = new ShallowRenderer();
  const tree = renderer.render(<DelegatedSearchPage location={{}} searchConfig={{}} />);
  expect(tree).toMatchSnapshot();
});

test('Renders other component if registered', () => {
  const renderer = new ShallowRenderer();
  const SimpleComponent = () => <div>Hello!</div>;

  PluginStore.register(new PluginManifest({}, {
    pages: {
      search: { component: SimpleComponent },
    },
  }));

  const tree = renderer.render(<DelegatedSearchPage />);

  expect(tree).toMatchSnapshot();
});
