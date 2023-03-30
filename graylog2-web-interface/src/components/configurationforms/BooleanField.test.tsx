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
import { screen, render, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { booleanField } from 'fixtures/configurationforms';

import BooleanField from './BooleanField';

describe('<BooleanField>', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof BooleanField>>) => (
    <BooleanField field={booleanField}
                  onChange={() => {}}
                  title="example_boolean_field"
                  typeName="boolean"
                  {...props} />
  );

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an unchecked field by default', () => {
    render(<SUT />);

    const fieldLabel = screen.getByText(booleanField.human_name, { exact: false });
    const optionalMarker = screen.queryByText(/(optional)/);
    const input = screen.getByLabelText(booleanField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).not.toBeInTheDocument();
    expect(input).toBeInTheDocument();
    expect(input).not.toBeChecked();
  });

  it('should render a checked field', () => {
    render(<SUT value />);

    const input = screen.getByLabelText(booleanField.human_name, { exact: false });

    expect(input).toBeInTheDocument();
    expect(input).toBeChecked();
  });

  it('should call onChange when input value changes', async () => {
    const changeFunction = jest.fn();
    const { rerender } = render(<SUT onChange={changeFunction} />);

    const input = screen.getByLabelText(booleanField.human_name, { exact: false });

    expect(input).toBeInTheDocument();
    expect(input).not.toBeChecked();

    userEvent.click(input);

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith('example_boolean_field', true));

    rerender(<SUT onChange={changeFunction} value />);

    expect(input).toBeChecked();

    userEvent.click(input);

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith('example_boolean_field', false));
  });
});
