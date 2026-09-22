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

import TemplateFieldValueProviderForm from './TemplateFieldValueProviderForm';

jest.mock('./TemplateFieldValueProviderPreview', () => () => <div>preview</div>);

describe('TemplateFieldValueProviderForm', () => {
  const configWith = (overrides: { require_values?: boolean; exclude_empty_fields?: boolean }) => ({
    providers: [
      {
        type: 'template-v1',
        template: 'hello',
        require_values: false,
        exclude_empty_fields: true,
        ...overrides,
      },
    ],
  });

  const defaultProps = {
    validation: { errors: {} },
  };

  it('defaults exclude_empty_fields to true for new providers', () => {
    expect(TemplateFieldValueProviderForm.defaultConfig).toEqual({ template: '', exclude_empty_fields: true });
  });

  it('disables the exclude empty fields checkbox when require values is checked', () => {
    render(
      <TemplateFieldValueProviderForm
        {...defaultProps}
        config={configWith({ require_values: true })}
        onChange={jest.fn()}
      />,
    );

    const checkbox = screen.getByRole('checkbox', { name: /exclude empty fields/i });

    expect(checkbox).toBeDisabled();
    expect(checkbox).toBeChecked();
  });

  it('forces exclude_empty_fields on when require values is checked', async () => {
    const onChange = jest.fn();

    render(
      <TemplateFieldValueProviderForm
        {...defaultProps}
        config={configWith({ exclude_empty_fields: false })}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByRole('checkbox', { name: /require all template values to be set/i }));

    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        providers: [expect.objectContaining({ require_values: true, exclude_empty_fields: true })],
      }),
    );
  });

  it('toggles exclude_empty_fields on its own', async () => {
    const onChange = jest.fn();

    render(
      <TemplateFieldValueProviderForm
        {...defaultProps}
        config={configWith({ exclude_empty_fields: true })}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByRole('checkbox', { name: /exclude empty fields/i }));

    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        providers: [expect.objectContaining({ exclude_empty_fields: false, require_values: false })],
      }),
    );
  });
});
