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
import { dropdownField, requiredDropdownField } from 'fixtures/configurationforms';
import userEvent from '@testing-library/user-event';

import DropdownField from './DropdownField';

describe('<DropdownField>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    const { container } = render(
      <DropdownField field={dropdownField}
                     onChange={() => {}}
                     title="example_dropdown_field"
                     typeName="dropdown"
                     autoFocus={false} />,
    );

    const fieldLabel = screen.getByText(dropdownField.human_name, { exact: true });
    const optionalMarker = screen.getByText(/(optional)/);
    const select = container.querySelector('select');

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(select).toBeInTheDocument();

    const selectedValue = Object.keys(dropdownField.attributes.values)[0];

    expect(select).toHaveValue(selectedValue);
    expect(select).not.toBeRequired();

    expect(container).toMatchSnapshot();
  });

  it('should render an empty field with placeholder', () => {
    const { container } = render(
      <DropdownField field={dropdownField}
                     onChange={() => {}}
                     title="example_dropdown_field"
                     typeName="dropdown"
                     autoFocus={false}
                     addPlaceholder />,
    );

    const select = container.querySelector('select');

    expect(select).toBeInTheDocument();
    expect(select).not.toHaveValue();
  });

  it('should render a required field', () => {
    const { container } = render(
      <DropdownField field={requiredDropdownField}
                     onChange={() => {}}
                     title="example_dropdown_field"
                     typeName="dropdown"
                     autoFocus={false}
                     addPlaceholder />,
    );

    const select = container.querySelector('select');

    expect(select).toBeInTheDocument();
    expect(select).not.toHaveValue();
    expect(select).toBeRequired();
  });

  it('should display options from attributes', () => {
    render(
      <DropdownField field={dropdownField}
                     onChange={() => {}}
                     title="example_dropdown_field"
                     typeName="dropdown"
                     autoFocus={false} />,
    );

    expect(screen.getByText('one')).toBeInTheDocument();
    expect(screen.getByText('two')).toBeInTheDocument();
  });

  it('should render a field with a value', async () => {
    const { container } = render(
      <DropdownField field={dropdownField}
                     onChange={() => {}}
                     title="example_dropdown_field"
                     typeName="dropdown"
                     autoFocus={false}
                     value="dos" />,
    );

    const select = container.querySelector('select');

    expect(select).toHaveValue('dos');
  });

  it('should call onChange when value changes', async () => {
    const updateFunction = jest.fn();

    const { container } = render(
      <DropdownField field={dropdownField}
                     onChange={updateFunction}
                     title="example_dropdown_field"
                     typeName="dropdown"
                     autoFocus={false} />,
    );

    const select = container.querySelector('select');

    userEvent.selectOptions(select, 'two');
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_dropdown_field', 'dos'));
  });
});
