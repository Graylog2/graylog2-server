// @flow strict
import React from 'react';
import { render } from 'wrappedTestingLibrary';

import User from 'logic/users/User';

import UserDetails from './UserDetails';

const user = User
  .builder()
  .fullName('The full name')
  .username('The username')
  .email('theemail@example.org')
  .clientAddress('127.0.0.1')
  .lastActivity('2020-01-01T10:40:05.376+0000')
  .sessionTimeoutMs(36000000)
  .timezone('Europe/Berlin')
  .build();

describe('<UserDetails />', () => {
  it('should display user profile', () => {
    const { getByText } = render(<UserDetails user={user} />);

    expect(getByText(user.username)).not.toBeNull();
    expect(getByText(user.fullName)).not.toBeNull();
    expect(getByText(user.email)).not.toBeNull();
    expect(getByText(user.clientAddress)).not.toBeNull();
    expect(getByText(user.lastActivity)).not.toBeNull();
  });

  describe('user settings', () => {
    it('should display timezone', () => {
      const { getByText } = render(<UserDetails user={user} />);

      expect(getByText(user.timezone)).not.toBeNull();
    });

    describe('should display session timeout in a readable format', () => {
      it('for seconds', () => {
        const test = user.toBuilder().sessionTimeoutMs(10000).build();
        const { getByText } = render(<UserDetails user={test} />);

        expect(getByText('10 Seconds')).not.toBeNull();
      });

      it('for minutes', () => {
        const { getByText } = render(<UserDetails user={user.toBuilder().sessionTimeoutMs(600000).build()} />);

        expect(getByText('10 Minutes')).not.toBeNull();
      });

      it('for hours', () => {
        const { getByText } = render(<UserDetails user={user.toBuilder().sessionTimeoutMs(36000000).build()} />);

        expect(getByText('10 Hours')).not.toBeNull();
      });

      it('for days', () => {
        const { getByText } = render(<UserDetails user={user.toBuilder().sessionTimeoutMs(864000000).build()} />);

        expect(getByText('10 Days')).not.toBeNull();
      });
    });
  });
});
