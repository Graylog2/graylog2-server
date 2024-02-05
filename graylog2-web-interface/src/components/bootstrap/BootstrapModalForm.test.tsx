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

import BootstrapModalForm from './BootstrapModalForm';

describe('BootstrapModalForm', () => {
  const children = <span>42</span>;

  const renderModalForm = (
    show: boolean = false,
    onSubmitForm: () => void = () => {},
    onCancel: () => void = () => {},
  ) => (
    <BootstrapModalForm title="Sample Form"
                        show={show}
                        onSubmitForm={onSubmitForm}
                        onCancel={onCancel}>
      {children}
    </BootstrapModalForm>
  );

  it('does not show modal form when show property is false', async () => {
    render(renderModalForm(false));

    expect(screen.queryByTitle('Sample Form')).not.toBeInTheDocument();
    expect(screen.queryByText('42')).not.toBeInTheDocument();
  });

  it('shows modal form when show property is true', async () => {
    render(renderModalForm(true));

    await screen.findByTitle('Sample Form');
    await screen.findByText('42');
  });

  it('calls onSubmit when form is submitted', async () => {
    const onCancel = jest.fn();
    const onSubmit = jest.fn();

    render(renderModalForm(true, onSubmit, onCancel));

    (await screen.findByRole('button', { name: 'Submit', hidden: true })).click();

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled();
    });

    expect(onCancel).not.toHaveBeenCalled();
  });

  it('calls onCancel when form is cancelled', async () => {
    const onCancel = jest.fn();
    const onSubmit = jest.fn();

    render(renderModalForm(true, onSubmit, onCancel));

    (await screen.findByRole('button', { name: 'Cancel', hidden: true })).click();

    await waitFor(() => {
      expect(onCancel).toHaveBeenCalled();
    });

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
