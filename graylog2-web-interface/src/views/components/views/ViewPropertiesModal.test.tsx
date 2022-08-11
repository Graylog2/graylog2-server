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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import View from 'views/logic/views/View';

import ViewPropertiesModal from './ViewPropertiesModal';

describe('ViewPropertiesModal', () => {
  it('should use updated view when saving', async () => {
    const onSave = jest.fn();
    const view = View.builder()
      .type(View.Type.Dashboard)
      .title('')
      .build();
    render(<ViewPropertiesModal onClose={jest.fn()} onSave={onSave} title="Saving new dashboard" view={view} show />);

    await screen.findByText('Saving new dashboard');
    const titleInput = await screen.findByRole('textbox', { name: /title/i, hidden: true });

    await userEvent.type(titleInput, 'My title');
    userEvent.click(await screen.findByRole('button', { name: 'Save', hidden: true }));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith(expect.objectContaining({
        title: 'My title',
      }));
    });
  });
});
