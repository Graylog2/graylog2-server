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
import userEvent from '@testing-library/user-event';
import { Formik } from 'formik';
import * as React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import JSONValueInput from './JSONValueInput';

type FormValues = { value: string; valueType: string };

const FormikJSONValueInput = ({ onChange }: { onChange: (values: FormValues) => void }) => (
  <Formik<FormValues>
    initialValues={{ value: '', valueType: 'STRING' }}
    validate={async () => ({})}
    onSubmit={() => {}}>
    {({ values, setFieldValue }) => {
      onChange(values);

      return (
        <JSONValueInput
          label="Default value"
          value={values.value}
          valueType={values.valueType}
          update={(value, valueType) => {
            setFieldValue('value', value);
            setFieldValue('valueType', valueType);
          }}
        />
      );
    }}
  </Formik>
);

describe('<JSONValueInput />', () => {
  it('keeps every typed character when the parent re-renders on async validation', async () => {
    const onChange = jest.fn();
    render(<FormikJSONValueInput onChange={onChange} />);

    const input = screen.getByRole('textbox');
    await userEvent.type(input, 'Outside Corp networks');

    await waitFor(() => expect(input).toHaveValue('Outside Corp networks'));
    expect(onChange).toHaveBeenLastCalledWith({ value: 'Outside Corp networks', valueType: 'STRING' });
  });

  it('keeps the current value when changing the value type', async () => {
    const onChange = jest.fn();
    render(<FormikJSONValueInput onChange={onChange} />);

    await userEvent.type(screen.getByRole('textbox'), '42');
    await userEvent.click(screen.getByRole('button', { name: /string/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /number/i }));

    await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ value: '42', valueType: 'NUMBER' }));
  });
});
