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
import { render, waitFor, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import authBindings from 'components/authentication/bindings';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import { asMock } from 'helpers/mocking';
import mockHistory from 'helpers/mocking/mockHistory';
import selectEvent from 'helpers/selectEvent';
import { usePluginExports } from 'views/test/testPlugins';

import ServiceSelect from './ServiceSelect';

jest.mock('routing/useHistory');

describe('ServiceSelect', () => {
  let history;

  usePluginExports(authBindings);

  beforeEach(() => {
    history = mockHistory();
    asMock(useHistory).mockReturnValue(history);
  });

  it('should redirect correctly after selecting LDAP', async () => {
    render(<ServiceSelect />);

    await selectEvent.chooseOption('Select a service', 'LDAP');

    const submitButton = await screen.findByRole('button', { name: 'Get started' });
    await userEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() =>
      expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend('ldap')),
    );
  });

  it('should redirect correctly after selecting active directory', async () => {
    render(<ServiceSelect />);

    await selectEvent.chooseOption('Select a service', 'Active Directory');

    const submitButton = await screen.findByRole('button', { name: 'Get started' });
    await userEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));
    await waitFor(() =>
      expect(history.push).toHaveBeenCalledWith(
        Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend('active-directory'),
      ),
    );
  });
});
