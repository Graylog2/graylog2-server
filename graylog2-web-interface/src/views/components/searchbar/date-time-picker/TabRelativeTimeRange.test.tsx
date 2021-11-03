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

import MockStore from 'helpers/mocking/StoreMock';
import MockAction from 'helpers/mocking/MockAction';
import { RELATIVE_CLASSIFIED_ALL_TIME_RANGE } from 'views/components/searchbar/date-time-picker/RelativeTimeRangeClassifiedHelper';

import TabRelativeTimeRange from './TabRelativeTimeRange';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
  ConfigurationsActions: {
    listSearchesClusterConfig: MockAction(),
  },
}));

const defaultProps = {
  limitDuration: 0,
  disabled: false,
};

const initialValues = {
  nextTimeRange: {
    type: 'relative',
    from: {
      value: 1,
      unit: 'hours',
      isAllTime: false,
    },
    to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
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

const fromRangeValue = () => screen.findByRole('spinbutton', { name: /set the from value/i });
const toRangeValue = () => screen.findByRole('spinbutton', { name: /set the to value/i });

describe('TabRelativeTimeRange', () => {
  it('renders initial from value', async () => {
    renderSUT();

    const spinbutton = await screen.findByRole('spinbutton', { name: /set the from value/i });

    expect(spinbutton).toBeInTheDocument();
    expect(spinbutton).toHaveValue(1);
  });

  it('sets "now" as default for to value', async () => {
    renderSUT();

    const allTimeCheckbox = await screen.findByRole('checkbox', { name: /Now/i });

    expect(allTimeCheckbox).toBeEnabled();
    expect(await toRangeValue()).toBeDisabled();
  });

  it('renders initial time range type', async () => {
    renderSUT();

    expect(await screen.findByText(/Hours/i)).toBeInTheDocument();
    expect((screen.getByRole('spinbutton', { name: /Set the from value/i }) as HTMLInputElement).value).toBe('1');
  });

  it('renders initial time range with from and to value', async () => {
    const initialFormValues = {
      ...initialValues,
      nextTimeRange: {
        ...initialValues.nextTimeRange,
        from: {
          value: 5,
          unit: 'minutes',
          isAllTime: false,
        },
        to: {
          value: 4,
          unit: 'minutes' as const,
          isAllTime: false,
        },
      },
    };
    renderSUT(undefined, initialFormValues);

    expect((await screen.findByRole('spinbutton', { name: /Set the from value/i }) as HTMLInputElement).value).toBe('5');
    expect((screen.getByRole('spinbutton', { name: /Set the to value/i }) as HTMLInputElement).value).toBe('4');
  });

  it('Clicking All Time disables input', async () => {
    renderSUT();

    const allTimeCheckbox = await screen.findByRole('checkbox', { name: /All Time/i });

    expect(await fromRangeValue()).not.toBeDisabled();

    fireEvent.click(allTimeCheckbox);

    expect(await fromRangeValue()).toBeDisabled();
  });

  it('Clicking Now enables to input', async () => {
    renderSUT();

    const nowCheckbox = await screen.findByRole('checkbox', { name: /Now/i });

    expect(await toRangeValue()).toBeDisabled();

    fireEvent.click(nowCheckbox);

    expect(await toRangeValue()).not.toBeDisabled();
  });

  it('All Time checkbox is disabled', async () => {
    renderSUT({ ...defaultProps, limitDuration: 10 });

    const allTimeCheckbox = await screen.findByRole('checkbox', { name: /All Time/i });

    expect(allTimeCheckbox).toBeDisabled();
  });
});
