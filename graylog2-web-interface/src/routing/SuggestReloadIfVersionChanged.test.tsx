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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import useServerVersion from 'routing/useServerVersion';

import SuggestReloadIfVersionChanged from './SuggestReloadIfVersionChanged';

jest.mock('./useServerVersion', () => jest.fn());

describe('SuggestReloadIfVersionChanged', () => {
  beforeEach(() => {
    asMock(useServerVersion).mockReturnValue('6.1.0');
  });

  it('shows dialog when version changes, reloads on click', async () => {
    const reload = jest.fn();
    const { rerender } = render(<SuggestReloadIfVersionChanged reload={reload} />);

    expect(screen.queryByText(/Your Graylog version has changed/i)).not.toBeInTheDocument();

    asMock(useServerVersion).mockReturnValue('6.2.0');

    rerender(<SuggestReloadIfVersionChanged reload={reload} />);

    await screen.findByText(/Your Graylog version has changed/i);

    const reloadButton = await screen.findByRole('button', { name: 'Reload now' });

    userEvent.click(reloadButton);

    expect(reload).toHaveBeenCalled();
  });
});
