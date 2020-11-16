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
// @flow strict
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import Footer from './Footer';

jest.mock('injection/StoreProvider', () => ({
  getStore: () => ({
    getInitialState: () => ({
      system: { version: '23.42.0-SNAPSHOT+SPECIALFEATURE', hostname: 'hopper.local' },
    }),
    jvm: jest.fn(() => Promise.resolve({ info: 'SomeJDK v12.0.0' })),
    listen: jest.fn(() => () => {}),
  }),
}));

describe('Footer', () => {
  it('includes Graylog version', async () => {
    const { findByText } = render(<Footer />);

    await findByText('Graylog 23.42.0-SNAPSHOT+SPECIALFEATURE', { exact: false });
  });

  it('includes hostname', async () => {
    const { findByText } = render(<Footer />);

    await findByText('on hopper.local', { exact: false });
  });

  it('includes JRE version', async () => {
    const { findByText } = render(<Footer />);

    await findByText('SomeJDK v12.0.0', { exact: false });
  });
});
