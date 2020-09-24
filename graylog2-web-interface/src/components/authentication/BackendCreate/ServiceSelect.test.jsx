// @flow strict
import * as React from 'react';
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import history from 'util/History';
import Routes from 'routing/Routes';

import ServiceSelect from './ServiceSelect';

jest.mock('util/History');

describe('ServiceSelect', () => {
  it('should redirect correctly after selecting LDAP ', async () => {
    const { getByLabelText, getByRole } = render(<ServiceSelect />);

    const serviceSelect = getByLabelText('Select a service');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(serviceSelect);
    await selectEvent.select(serviceSelect, 'LDAP');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend('ldap')));
  });

  it('should redirect correctly after selecting active directory ', async () => {
    const { getByLabelText, getByRole } = render(<ServiceSelect />);

    const serviceSelect = getByLabelText('Select a service');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(serviceSelect);
    await selectEvent.select(serviceSelect, 'Active Directory');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend('active-directory')));
  });
});
