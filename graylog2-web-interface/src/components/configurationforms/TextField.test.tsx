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
import React from 'react';
import { screen, render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import { passwordTextField, requiredTextField, textAreaField, textField } from 'fixtures/configurationforms';

import TextField from './TextField';

describe('<TextField>', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof TextField>>) => (
    <TextField field={textField}
               onChange={() => {}}
               title="example_text_field"
               typeName="text"
               {...props} />
  );

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    render(<SUT />);

    const fieldLabel = screen.getByText(textField.human_name, { exact: false });
    const optionalMarker = screen.getByText(/(optional)/);
    const formField = screen.getByLabelText(textField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(formField).toBeInTheDocument();
    expect(formField).not.toHaveValue();
    expect(formField).not.toHaveAttribute('required');
    expect(formField).toHaveAttribute('type', 'text');
  });

  it('should render a field with a value', () => {
    render(<SUT value={textField.default_value} />);

    const formField = screen.getByLabelText(textField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveValue(textField.default_value);
  });

  it('should render a required text field', () => {
    render(<SUT field={requiredTextField} />);

    const fieldLabel = screen.getByText(textField.human_name, { exact: false });
    const optionalMarker = screen.queryByText(/(optional)/);
    const formField = screen.getByLabelText(textField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).not.toBeInTheDocument();
    expect(formField).toBeInTheDocument();
    expect(formField).not.toHaveValue();
    expect(formField).toHaveAttribute('required');
  });

  it('should call onChange when value changes', async () => {
    const updateFunction = jest.fn();

    render(<SUT onChange={updateFunction} value={textField.default_value} />);

    const formField = screen.getByLabelText(textField.human_name, { exact: false });

    fireEvent.change(formField, { target: { value: 'new value' } });

    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_text_field', 'new value'));
  });

  it('should render a password field', () => {
    render(<SUT field={passwordTextField} title="example_password_field" />);

    const formField = screen.getByLabelText(passwordTextField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('type', 'password');
  });

  it('should render a textarea field', () => {
    render(<SUT field={textAreaField} />);

    const formField = screen.getByLabelText(textAreaField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
  });
});
