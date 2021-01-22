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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import TabRelativeTimeRange from './TabRelativeTimeRange';

const defaultProps = {
  limitDuration: 0,
  disabled: false,
};

const initialValues = {
  nextTimeRange: {
    type: 'relative',
    from: 3600,
  },
};

const renderSUT = (allProps = defaultProps, initialFormValues = initialValues) => render(
  <Formik initialValues={initialFormValues}
          onSubmit={() => {}}
          validateOnMount>
    <Form>
      <TabRelativeTimeRange {...allProps} />
    </Form>
  </Formik>,
);

describe('TabRelativeTimeRange', () => {
  it('renders initial from value', () => {
    renderSUT();

    const spinbutton = screen.getByRole('spinbutton', { name: /set the from value/i });

    expect(spinbutton).toBeInTheDocument();
    expect(spinbutton).toHaveValue(1);
  });

  it('sets "now" as default for to value', () => {
    renderSUT();

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /Now/i });
    const toRangeValue = screen.getByRole('spinbutton', { name: /set the to value/i });

    expect(allTimeCheckbox).toBeEnabled();
    expect(toRangeValue).toBeDisabled();
  });

  it('renders initial time range type', () => {
    renderSUT();

    expect(screen.getByText(/Hours/i)).toBeInTheDocument();
    expect((screen.getByRole('spinbutton', { name: /Set the from value/i }) as HTMLInputElement).value).toBe('1');
  });

  it('renders initial time range with from and to value', () => {
    const initialFormValues = {
      ...initialValues,
      nextTimeRange: {
        ...initialValues.nextTimeRange,
        from: 300,
        to: 240,
      },
    };
    renderSUT(undefined, initialFormValues);

    expect((screen.getByRole('spinbutton', { name: /Set the from value/i }) as HTMLInputElement).value).toBe('5');
    expect((screen.getByRole('spinbutton', { name: /Set the to value/i }) as HTMLInputElement).value).toBe('4');
  });

  it('Clicking All Time disables input', () => {
    renderSUT();

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });
    const fromRangeValue = screen.getByRole('spinbutton', { name: /set the from value/i });

    expect(fromRangeValue).not.toBeDisabled();

    fireEvent.click(allTimeCheckbox);

    expect(fromRangeValue).toBeDisabled();
  });

  it('Clicking Now enables to input', () => {
    renderSUT();

    const nowCheckbox = screen.getByRole('checkbox', { name: /Now/i });
    const toRangeValue = screen.getByRole('spinbutton', { name: /set the to value/i });

    expect(toRangeValue).toBeDisabled();

    fireEvent.click(nowCheckbox);

    expect(toRangeValue).not.toBeDisabled();
  });

  it('All Time checkbox is disabled', () => {
    renderSUT({ ...defaultProps, limitDuration: 10 });

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });

    expect(allTimeCheckbox).toBeDisabled();
  });
});
