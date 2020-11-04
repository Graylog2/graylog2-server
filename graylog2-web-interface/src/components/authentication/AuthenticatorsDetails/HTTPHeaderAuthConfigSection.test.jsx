import React from 'react';
import { render, screen, act } from 'wrappedTestingLibrary';

import HTTPHeaderAuthConfig from 'logic/authentication/HTTPHeaderAuthConfig';

import HTTPHeaderAuthConfigSection from './HTTPHeaderAuthConfigSection';

const mockHTTPHeaderAuthConfig = HTTPHeaderAuthConfig.builder()
  .usernameHeader('Remote-User')
  .enabled(true)
  .build();

jest.mock('stores/authentication/HTTPHeaderAuthConfigStore', () => ({
  HTTPHeaderAuthConfigActions: {
    load: jest.fn(() => Promise.resolve(mockHTTPHeaderAuthConfig)),
  },
}));

jest.useFakeTimers();

describe('<HTTPHeaderAuthConfigSection />', () => {
  it('should display loading indicator while loading', async () => {
    render(<HTTPHeaderAuthConfigSection />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should load and display HTTP header auth config details', async () => {
    render(<HTTPHeaderAuthConfigSection />);

    await screen.findByText('Enabled');

    expect(screen.getByText('yes')).toBeInTheDocument();
    expect(screen.getByText('Remote-User')).toBeInTheDocument();
  });
});
