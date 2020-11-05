// @flow strict
import * as React from 'react';
import { render, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';
import { alice } from 'fixtures/users';

import ProfileSection from './ProfileSection';

const exampleUser = alice.toBuilder()
  .username('johndoe')
  .fullName('John Doe')
  .email('johndoe@example.org')
  .build();

describe('<ProfileSection />', () => {
  it('should display username', async () => {
    render(<ProfileSection user={exampleUser} onSubmit={jest.fn()} />);

    expect(screen.getByText(exampleUser.username)).toBeInTheDocument();
  });

  it('should use user details as initial values', async () => {
    const onSubmitStub = jest.fn();
    render(<ProfileSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);

    const submitButton = screen.getByText('Update Profile');
    fireEvent.click(submitButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({
      full_name: exampleUser.fullName,
      email: exampleUser.email,
    });
  });

  it('should allow full name and e-mail address change', async () => {
    const onSubmitStub = jest.fn();
    render(<ProfileSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);

    const fullNameInput = screen.getByLabelText('Full Name');
    const emailInput = screen.getByLabelText('E-Mail Address');
    const submitButton = screen.getByText('Update Profile');

    fireEvent.change(fullNameInput, { target: { value: 'New full name' } });
    fireEvent.change(emailInput, { target: { value: 'newfullname@example.org' } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({
      full_name: 'New full name',
      email: 'newfullname@example.org',
    });
  });
});
