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
import selectEvent from 'react-select-event';
import { creatableListField, listField } from 'fixtures/configurationforms';

import ListField from './ListField';

describe('<ListField>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    const { container } = render(
      <ListField field={listField}
                 onChange={() => {}}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const fieldLabel = screen.getByText(listField.human_name, { exact: true });
    const optionalMarker = screen.getByText(/(optional)/);
    const select = screen.getByLabelText(listField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(select).toBeInTheDocument();

    expect(container).toMatchSnapshot();
  });

  it('should display options from attributes', async () => {
    render(
      <ListField field={listField}
                 onChange={() => {}}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const select = screen.getByLabelText(listField.human_name, { exact: false });

    expect(screen.queryByText('uno')).not.toBeInTheDocument();
    expect(screen.queryByText('dos')).not.toBeInTheDocument();

    await selectEvent.openMenu(select);

    expect(screen.getByText('uno')).toBeInTheDocument();
    expect(screen.getByText('dos')).toBeInTheDocument();
  });

  it('should render a field with values', async () => {
    render(
      <ListField field={listField}
                 onChange={() => {}}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false}
                 value={['one', 'two']} />,
    );

    expect(screen.getByText('uno')).toBeInTheDocument();
    expect(screen.getByText('dos')).toBeInTheDocument();
  });

  it('should call onChange when value changes', async () => {
    const updateFunction = jest.fn();

    render(
      <ListField field={listField}
                 onChange={updateFunction}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const select = screen.getByLabelText(listField.human_name, { exact: false });

    await selectEvent.select(select, ['uno', 'dos']);
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_list_field', ['one', 'two']));
  });

  it('should call onChange when clearing values', async () => {
    const updateFunction = jest.fn();

    render(
      <ListField field={listField}
                 onChange={updateFunction}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false}
                 value={['one']} />,
    );

    const select = screen.getByLabelText(listField.human_name, { exact: false });

    await selectEvent.clearAll(select);
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_list_field', []));
  });

  it('should create new values when allow_create is set', async () => {
    const updateFunction = jest.fn();

    render(
      <ListField field={creatableListField}
                 onChange={updateFunction}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const select = screen.getByLabelText(listField.human_name, { exact: false });

    await selectEvent.create(select, 'three');
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_list_field', ['three']));
  });
});
