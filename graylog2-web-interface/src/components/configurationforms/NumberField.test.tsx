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
    const { container } = render(
      <NumberField field={numberField}
                   onChange={() => {}}
                   title={numberField.title}
                   typeName="number" />,
    );

    const fieldLabel = screen.getByText(numberField.human_name, { exact: false });
    const optionalMarker = screen.getByText(/(optional)/);
    const formField = container.querySelector('input[type="number"]');
    // const formField = screen.getByLabelText(/number field/i);

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).toBeInTheDocument();
    expect(formField).toBeInTheDocument();
    expect(formField).not.toHaveValue();
    expect(formField).not.toBeRequired();
    expect(formField).toHaveAttribute('max', String(Number.MAX_SAFE_INTEGER));
    expect(formField).toHaveAttribute('min', String(Number.MIN_SAFE_INTEGER));
  });

  it('should render a field with a value', () => {
    const { container } = render(
      <NumberField field={numberField}
                   onChange={() => {}}
                   title={numberField.title}
                   typeName="number"
                   value={numberField.default_value} />,
    );

    const formField = container.querySelector('input[type="number"]');

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveValue(numberField.default_value);
  });

  it('should render a required number field', () => {
    const { container } = render(
      <NumberField field={requiredNumberField}
                   onChange={() => {}}
                   title={requiredNumberField.title}
                   typeName="number" />,
    );

    const fieldLabel = screen.getByText(requiredNumberField.human_name, { exact: false });
    const optionalMarker = screen.queryByText(/(optional)/);
    const formField = container.querySelector('input[type="number"]');
    // const formField = screen.getByLabelText(/number field/i);

    expect(fieldLabel).toBeInTheDocument();
    expect(optionalMarker).not.toBeInTheDocument();
    expect(formField).toBeInTheDocument();
    expect(formField).toBeRequired();
  });

  it('should call onChange when the value changes', async () => {
    const changeFunction = jest.fn();

    const { container } = render(
      <NumberField field={numberField}
                   onChange={changeFunction}
                   title={numberField.title}
                   typeName="number"
                   value={numberField.default_value} />,
    );

    const formField = container.querySelector('input[type="number"]');
    fireEvent.change(formField, { target: { value: '123' } });

    // userEvent.clear(formField);
    // userEvent.type(formField, '123');

    await waitFor(() => expect(changeFunction).toHaveBeenCalledWith(numberField.title, 123));
  });

  it('should render negative number field', () => {
    const { container } = render(
      <NumberField field={negativeNumberField}
                   onChange={() => {}}
                   title={negativeNumberField.title}
                   typeName="number" />,
    );

    const formField = container.querySelector('input[type="number"]');
    // const formField = screen.getByLabelText(/number field/i);

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('max', '-1');
    expect(formField).toHaveAttribute('min', String(Number.MIN_SAFE_INTEGER));
  });

  it('should render positive number field', () => {
    const { container } = render(
      <NumberField field={positiveNumberField}
                   onChange={() => {}}
                   title={positiveNumberField.title}
                   typeName="number" />,
    );

    const formField = container.querySelector('input[type="number"]');
    // const formField = screen.getByLabelText(/number field/i);

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('max', String(Number.MAX_SAFE_INTEGER));
    expect(formField).toHaveAttribute('min', '0');
  });

  it('should render port number field', () => {
    const { container } = render(
      <NumberField field={portNumberField}
                   onChange={() => {}}
                   title={portNumberField.title}
                   typeName="number" />,
    );

    const formField = container.querySelector('input[type="number"]');
    // const formField = screen.getByLabelText(/number field/i);

    expect(formField).toBeInTheDocument();
    expect(formField).toHaveAttribute('max', '65535');
    expect(formField).toHaveAttribute('min', '0');
  });

  it('should match the snapshot', () => {
    const { container } = render(<NumberField field={numberField}
                                              onChange={() => {}}
                                              title={numberField.title}
                                              typeName="number" />);

    expect(container).toMatchSnapshot();
  });
});
