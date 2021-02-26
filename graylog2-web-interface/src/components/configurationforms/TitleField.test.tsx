import React from 'react';
import { screen, render, waitFor, fireEvent } from 'wrappedTestingLibrary';

import TitleField from './TitleField';

describe('<TitleField>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    const { container } = render(
      <TitleField typeName="org.graylog.plugins.example" />,
    );

    const titleField = container.querySelector('input[id="title"]');
    // const titleField = screen.getByLabelText(/title/i);

    expect(titleField).toBeInTheDocument();
    expect(titleField).toHaveAttribute('required');
    expect(titleField).not.toHaveValue();
  });

  it('should render a field with value', () => {
    const { container } = render(
      <TitleField typeName="org.graylog.plugins.example" value="My title" />,
    );

    const titleField = container.querySelector('input[id="title"]');
    // const titleField = screen.getByLabelText(/title/i);

    expect(titleField).toHaveValue('My title');
  });

  it('should call onChange function when input value changes', async () => {
    const changeFunction = jest.fn();

    const { container } = render(
      <TitleField typeName="org.graylog.plugins.example" onChange={changeFunction} />,
    );

    const titleField = container.querySelector('input[id="title"]');
    // const titleField = screen.getByLabelText(/title/i);
    fireEvent.change(titleField, { target: { value: 'New title' } });

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith('title', 'New title'));
  });
});
