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
