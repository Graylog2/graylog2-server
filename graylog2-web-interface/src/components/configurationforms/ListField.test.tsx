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
    const select: HTMLElement = container.querySelector('input[type="text"]');

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(select).toBeInTheDocument();

    expect(container).toMatchSnapshot();
  });

  it('should display options from attributes', async () => {
    const { container } = render(
      <ListField field={listField}
                 onChange={() => {}}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const select: HTMLElement = container.querySelector('input[type="text"]');

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

    const { container } = render(
      <ListField field={listField}
                 onChange={updateFunction}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const select: HTMLElement = container.querySelector('input[type="text"]');

    await selectEvent.select(select, ['uno', 'dos']);
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_list_field', ['one', 'two']));
  });

  it('should call onChange when clearing values', async () => {
    const updateFunction = jest.fn();

    const { container } = render(
      <ListField field={listField}
                 onChange={updateFunction}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false}
                 value={['one']} />,
    );

    const select: HTMLElement = container.querySelector('input[type="text"]');

    await selectEvent.clearAll(select);
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_list_field', []));
  });

  it('should create new values when allow_create is set', async () => {
    const updateFunction = jest.fn();

    const { container } = render(
      <ListField field={creatableListField}
                 onChange={updateFunction}
                 title="example_list_field"
                 typeName="list"
                 autoFocus={false} />,
    );

    const select: HTMLElement = container.querySelector('input[type="text"]');

    await selectEvent.create(select, 'three');
    await waitFor(() => expect(updateFunction).toHaveBeenCalledWith('example_list_field', ['three']));
  });
});
