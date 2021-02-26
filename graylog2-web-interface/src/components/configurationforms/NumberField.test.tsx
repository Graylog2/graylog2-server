import React from 'react';
import { fireEvent, screen, render, waitFor } from 'wrappedTestingLibrary';
import {
  negativeNumberField,
  numberField,
  portNumberField,
  positiveNumberField,
  requiredNumberField,
} from 'fixtures/configurationforms';

import NumberField from './NumberField';

describe('<NumberField>', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('should render an empty field', () => {
    render(
      <NumberField field={numberField}
                   onChange={() => {}}
                   title="example_number_field"
                   typeName="number" />,
    );

    const fieldLabel = screen.getByText(numberField.human_name, { exact: false });
    const optionalMarker = screen.getByText(/(optional)/);
    const formField = screen.getByLabelText(numberField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(formField).toBeInTheDocument();
    expect(formField).not.toHaveValue();
    expect(formField).not.toBeRequired();
    expect(formField).toHaveAttribute('max', String(Number.MAX_SAFE_INTEGER));
    expect(formField).toHaveAttribute('min', String(Number.MIN_SAFE_INTEGER));
  });

  it('should render a field with a value', () => {
    render(
      <NumberField field={numberField}
                   onChange={() => {}}
                   title="example_number_field"
                   typeName="number"
                   value={numberField.default_value} />,
    );

    const formField = screen.getByLabelText(numberField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveValue(numberField.default_value);
  });

  it('should render a required number field', () => {
    render(
      <NumberField field={requiredNumberField}
                   onChange={() => {}}
                   title="example_number_field"
                   typeName="number" />,
    );

    const fieldLabel = screen.getByText(requiredNumberField.human_name, { exact: false });
    const optionalMarker = screen.queryByText(/(optional)/);
    const formField = screen.getByLabelText(requiredNumberField.human_name, { exact: false });

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).not.toBeInTheDocument();
    expect(formField).toBeInTheDocument();
    expect(formField).toBeRequired();
  });

  it('should call onChange when the value changes', async () => {
    const changeFunction = jest.fn();

    render(
      <NumberField field={numberField}
                   onChange={changeFunction}
                   title="example_number_field"
                   typeName="number"
                   value={numberField.default_value} />,
    );

    const formField = screen.getByLabelText(numberField.human_name, { exact: false });
    fireEvent.change(formField, { target: { value: '123' } });

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith('example_number_field', 123));
  });

  it('should render negative number field', () => {
    render(
      <NumberField field={negativeNumberField}
                   onChange={() => {}}
                   title="example_number_field"
                   typeName="number" />,
    );

    const formField = screen.getByLabelText(negativeNumberField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('max', '-1');
    expect(formField).toHaveAttribute('min', String(Number.MIN_SAFE_INTEGER));
  });

  it('should render positive number field', () => {
    render(
      <NumberField field={positiveNumberField}
                   onChange={() => {}}
                   title="example_number_field"
                   typeName="number" />,
    );

    const formField = screen.getByLabelText(positiveNumberField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('max', String(Number.MAX_SAFE_INTEGER));
    expect(formField).toHaveAttribute('min', '0');
  });

  it('should render port number field', () => {
    render(
      <NumberField field={portNumberField}
                   onChange={() => {}}
                   title="example_number_field"
                   typeName="number" />,
    );

    const formField = screen.getByLabelText(portNumberField.human_name, { exact: false });

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('max', '65535');
    expect(formField).toHaveAttribute('min', '0');
  });

  it('should match the snapshot', () => {
    const { container } = render(<NumberField field={numberField}
                                              onChange={() => {}}
                                              title="example_number_field"
                                              typeName="number" />);

    expect(container).toMatchSnapshot();
  });
});
