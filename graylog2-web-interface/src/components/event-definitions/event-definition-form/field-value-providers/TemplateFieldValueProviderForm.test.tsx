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
  const configWith = (overrides: { require_values?: boolean; include_empty_fields?: boolean }) => ({
    providers: [
      {
        type: 'template-v1',
        template: 'hello',
        require_values: false,
        include_empty_fields: false,
        ...overrides,
      },
    ],
  });

  const defaultProps = {
    validation: { errors: {} },
  };

  it('defaults include_empty_fields to false for new providers', () => {
    expect(TemplateFieldValueProviderForm.defaultConfig).toEqual({ template: '', include_empty_fields: false });
  });

  it('disables the include empty fields checkbox when require values is checked', () => {
    render(
      <TemplateFieldValueProviderForm
        {...defaultProps}
        config={configWith({ require_values: true })}
        onChange={jest.fn()}
      />,
    );

    expect(screen.getByRole('checkbox', { name: /include empty fields/i })).toBeDisabled();
  });

  it('clears include_empty_fields when require values is checked', async () => {
    const onChange = jest.fn();

    render(
      <TemplateFieldValueProviderForm
        {...defaultProps}
        config={configWith({ include_empty_fields: true })}
        onChange={onChange}
      />,
    );

    await userEvent.click(screen.getByRole('checkbox', { name: /require all template values to be set/i }));

    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        providers: [expect.objectContaining({ require_values: true, include_empty_fields: false })],
      }),
    );
  });

  it('toggles include_empty_fields on its own', async () => {
    const onChange = jest.fn();

    render(<TemplateFieldValueProviderForm {...defaultProps} config={configWith({})} onChange={onChange} />);

    await userEvent.click(screen.getByRole('checkbox', { name: /include empty fields/i }));

    expect(onChange).toHaveBeenCalledWith(
      expect.objectContaining({
        providers: [expect.objectContaining({ include_empty_fields: true, require_values: false })],
      }),
    );
  });
});
