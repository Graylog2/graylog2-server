// @flow strict
import * as React from 'react';
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import history from 'util/History';
import Routes from 'routing/Routes';

import BackendCreateSelect from './BackendCreateSelect';

jest.mock('util/History');

describe('BackendCreateSelect', () => {
  it('should redirect correctly after selecting LDAP ', async () => {
    const { getByLabelText, getByRole } = render(<BackendCreateSelect />);

    const serviceSelect = getByLabelText('Select a service');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(serviceSelect);
    await selectEvent.select(serviceSelect, 'LDAP');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.SERVICE.CREATE_LDAP));
  });

  it('should redirect correctly after selecting active directory ', async () => {
    const { getByLabelText, getByRole } = render(<BackendCreateSelect />);

    const serviceSelect = getByLabelText('Select a service');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(serviceSelect);
    await selectEvent.select(serviceSelect, 'Active Directory');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.SERVICE.CREATE_AD));
  });
});
