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
import { screen, render, waitFor, fireEvent } from 'wrappedTestingLibrary';

import TitleField from './TitleField';

describe('<TitleField>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    render(
      <TitleField typeName="org.graylog.plugins.example" />,
    );

    const titleField = screen.getByLabelText(/title/i);

    expect(titleField).toBeInTheDocument();
    expect(titleField).toHaveAttribute('required');
    expect(titleField).not.toHaveValue();
  });

  it('should render a field with value', () => {
    render(
      <TitleField typeName="org.graylog.plugins.example" value="My title" />,
    );

    const titleField = screen.getByLabelText(/title/i);

    expect(titleField).toHaveValue('My title');
  });

  it('should call onChange function when input value changes', async () => {
    const changeFunction = jest.fn();

    render(
      <TitleField typeName="org.graylog.plugins.example" onChange={changeFunction} />,
    );

    const titleField = screen.getByLabelText(/title/i);
    fireEvent.change(titleField, { target: { value: 'New title' } });

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith('title', 'New title'));
  });
});
