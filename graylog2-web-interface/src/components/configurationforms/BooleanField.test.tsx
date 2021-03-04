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
import { booleanField } from 'fixtures/configurationforms';
import userEvent from '@testing-library/user-event';

import BooleanField from './BooleanField';

describe('<BooleanField>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an unchecked field by default', () => {
    const { container } = render(
      <BooleanField field={booleanField}
                    onChange={() => {}}
                    title="example_boolean_field"
                    typeName="boolean" />,
    );

    const fieldLabel = screen.getByText(booleanField.human_name, { exact: false });
    const optionalMarker = screen.queryByText(/(optional)/);
    const input = screen.getByLabelText(booleanField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).not.toBeInTheDocument();
    expect(input).toBeInTheDocument();
    expect(input).not.toBeChecked();

    expect(container).toMatchSnapshot();
  });

  it('should render a checked field', () => {
    const { container } = render(
      <BooleanField field={booleanField}
                    onChange={() => {}}
                    title="example_boolean_field"
                    typeName="boolean"
                    value />,
    );

    const input = screen.getByLabelText(booleanField.human_name, { exact: false });

    expect(input).toBeInTheDocument();
    expect(input).toBeChecked();

    expect(container).toMatchSnapshot();
  });

  it('should call onChange when input value changes', async () => {
    const changeFunction = jest.fn();

    render(
      <BooleanField field={booleanField}
                    onChange={changeFunction}
                    title="example_boolean_field"
                    typeName="boolean" />,
    );

    const input = screen.getByLabelText(booleanField.human_name, { exact: false });

    expect(input).toBeInTheDocument();
    expect(input).not.toBeChecked();

    userEvent.click(input);

    expect(input).toBeChecked();

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith('example_boolean_field', true));
  });
});
