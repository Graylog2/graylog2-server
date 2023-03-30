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
import * as React from 'react';
import { render, waitFor, fireEvent } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import 'components/authentication/bindings'; // Bind all authentication plugins
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import { asMock } from 'helpers/mocking';
import mockHistory from 'helpers/mocking/mockHistory';

import ServiceSelect from './ServiceSelect';

jest.mock('routing/useHistory');

describe('ServiceSelect', () => {
  let history;

  beforeEach(() => {
    history = mockHistory();
    asMock(useHistory).mockReturnValue(history);
  });

  it('should redirect correctly after selecting LDAP', async () => {
    const { getByLabelText, getByRole } = render(<ServiceSelect />);

    const serviceSelect = getByLabelText('Select a service');
    const submitButton = getByRole('button', { name: 'Get started' });
    await selectEvent.openMenu(serviceSelect);
    await selectEvent.select(serviceSelect, 'LDAP');

    fireEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend('ldap')));
  });

  it('should redirect correctly after selecting active directory', async () => {
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
