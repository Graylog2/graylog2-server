// @flow strict
import * as React from 'react';
import { render, act, screen } from 'wrappedTestingLibrary';
import { alertsManager as exampleRole } from 'fixtures/roles';

import RoleEdit from './RoleEdit';

jest.mock('./UsersSection', () => () => <div>UsersSection</div>);

jest.useFakeTimers();

describe('RoleEdit', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should display loading indicator, if no role is provided', async () => {
    render(<RoleEdit role={undefined} />);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    await screen.findByText('Loading...');
  });

  it('should display role profile', () => {
    render(<RoleEdit role={exampleRole} />);

    expect(screen.getByText(exampleRole.name)).toBeInTheDocument();
    expect(screen.getByText(exampleRole.description)).toBeInTheDocument();
  });

  it('should display users section', () => {
    render(<RoleEdit role={exampleRole} />);

    expect(screen.getByText('UsersSection')).toBeInTheDocument();
  });
});
