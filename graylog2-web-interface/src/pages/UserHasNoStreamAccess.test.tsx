import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import mockComponent from 'helpers/mocking/MockComponent';

import UserHasNoStreamAccess from 'pages/UserHasNoStreamAccess';

jest.mock('components/layout/Footer', () => mockComponent('Footer'));

describe('UserHasNoStreamAccess', () => {
  it('should render error message', () => {
    render(<UserHasNoStreamAccess />);
    screen.findByText(/We cannot start a search right now, because you are not allowed to access any stream./);
  });
});
