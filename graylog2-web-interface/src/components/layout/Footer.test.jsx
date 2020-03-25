// @flow strict
import * as React from 'react';
import { cleanup, render, waitForElement } from 'wrappedTestingLibrary';

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
  afterEach(cleanup);
  it('includes Graylog version', async () => {
    const { getByText } = render(<Footer />);
    await waitForElement(() => getByText('Graylog 23.42.0-SNAPSHOT+SPECIALFEATURE', { exact: false }));
  });
  it('includes hostname', async () => {
    const { getByText } = render(<Footer />);
    await waitForElement(() => getByText('on hopper.local', { exact: false }));
  });
  it('includes JRE version', async () => {
    const { getByText } = render(<Footer />);
    await waitForElement(() => getByText('SomeJDK v12.0.0', { exact: false }));
  });
});
