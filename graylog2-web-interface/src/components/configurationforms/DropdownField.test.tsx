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

import { dropdownField, requiredDropdownField } from 'fixtures/configurationforms';

import DropdownField from './DropdownField';

describe('<DropdownField>', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof DropdownField>>) => (
    <DropdownField field={dropdownField}
                   onChange={() => {}}
                   title="example_dropdown_field"
                   typeName="dropdown"
                   autoFocus={false}
                   {...props} />
  );

  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    render(<SUT />);

    const fieldLabel = screen.getByText(dropdownField.human_name, { exact: true });
    const optionalMarker = screen.getByText(/(optional)/);
    const select = screen.getByLabelText(dropdownField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(select).toBeInTheDocument();

    const selectedValue = Object.keys(dropdownField.attributes.values)[0];

    expect(select).toHaveValue(selectedValue);
    expect(select).not.toBeRequired();
  });

  it('should render an empty field with placeholder', () => {
    render(<SUT addPlaceholder />);

    const select = screen.getByLabelText(dropdownField.human_name, { exact: false });

    expect(select).toBeInTheDocument();
    expect(select).not.toHaveValue();
  });

  it('should render a required field', () => {
    render(
      <SUT field={requiredDropdownField}
           addPlaceholder />,
    );

    const select = screen.getByLabelText(dropdownField.human_name, { exact: false });

    expect(select).toBeInTheDocument();
    expect(select).not.toHaveValue();
    expect(select).toBeRequired();
  });

  it('should display options from attributes', () => {
    render(<SUT />);

    expect(screen.getByText('one')).toBeInTheDocument();
    expect(screen.getByText('two')).toBeInTheDocument();
  });

  it('should render a field with a value', async () => {
    render(<SUT value="dos" />);

    const select = screen.getByLabelText(dropdownField.human_name, { exact: false });

    expect(select).toHaveValue('dos');
  });

  it('should call onChange when value changes', async () => {
    const updateFunction = jest.fn();

    render(<SUT onChange={updateFunction} />);

    const select = screen.getByLabelText(dropdownField.human_name, { exact: false });

    userEvent.selectOptions(select, 'two');
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_dropdown_field', 'dos'));
  });
});
