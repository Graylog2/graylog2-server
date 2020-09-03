// @flow strict
import * as React from 'react';
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import history from 'util/History';
import Routes from 'routing/Routes';

import ProviderCreateSelect from './ProviderCreateSelect';

jest.mock('util/History');

describe('ProviderGettingStarted', () => {
  it('should redirect correctly after selecting LDAP ', async () => {
    const { getByLabelText, getByRole } = render(<ProviderCreateSelect />);

    const providerSelect = getByLabelText('Select a provider');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(providerSelect);
    await selectEvent.select(providerSelect, 'LDAP');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE_LDAP));
  });

  it('should redirect correctly after selecting active directory ', async () => {
    const { getByLabelText, getByRole } = render(<ProviderCreateSelect />);

    const providerSelect = getByLabelText('Select a provider');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(providerSelect);
    await selectEvent.select(providerSelect, 'Active Directory');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE_AD));
  });
});
