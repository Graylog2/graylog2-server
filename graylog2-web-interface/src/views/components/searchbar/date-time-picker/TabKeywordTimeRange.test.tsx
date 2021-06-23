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
import { asElement, fireEvent, render, waitFor } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';
import { act } from 'react-dom/test-utils';
import asMock from 'helpers/mocking/AsMock';

import ToolsStore from 'stores/tools/ToolsStore';

import OriginalTabKeywordTimeRange from './TabKeywordTimeRange';

jest.mock('stores/tools/ToolsStore', () => ({}));

const TabKeywordTimeRange = ({ defaultValue, ...props }: { defaultValue: string } & React.ComponentProps<typeof TabKeywordTimeRange>) => (
  <Formik initialValues={{ nextTimeRange: { type: 'keyword', keyword: defaultValue } }}
          onSubmit={() => {}}
          validateOnMount>
    <Form>
      <OriginalTabKeywordTimeRange {...props as React.ComponentProps<typeof TabKeywordTimeRange>} />
    </Form>
  </Formik>
);

jest.mock('logic/datetimes/DateTime', () => ({ fromUTCDateTime: (date) => date }));

describe('TabKeywordTimeRange', () => {
  beforeEach(() => {
    ToolsStore.testNaturalDate = jest.fn(() => Promise.resolve({
      from: '2018-11-14 13:52:38',
      to: '2018-11-14 13:57:38',
    }));
  });

  const findValidationState = (container) => {
    const formGroup = container.querySelector('.form-group');

    return formGroup && formGroup.className.includes('has-error')
      ? 'error'
      : null;
  };

  const changeInput = async (input, value) => act(async () => {
    const { name } = asElement(input, HTMLInputElement);

    fireEvent.change(input, { target: { value, name } });
  });

  const asyncRender = async (element) => {
    let wrapper;

    await act(async () => { wrapper = render(element); });

    if (!wrapper) {
      throw new Error('Render returned `null`.');
    }

    return wrapper;
  };

  it('renders value passed to it', async () => {
    const { getByDisplayValue } = await asyncRender(<TabKeywordTimeRange defaultValue="Last hour" />);

    expect(getByDisplayValue('Last hour')).not.toBeNull();
  });

  it('calls onChange if value changes', async () => {
    const { getByDisplayValue } = await asyncRender(<TabKeywordTimeRange defaultValue="Last hour" />);
    const input = getByDisplayValue('Last hour');

    await changeInput(input, 'last year');

    expect(input).toHaveValue('last year');
  });

  it('calls testNaturalDate', async () => {
    expect(ToolsStore.testNaturalDate).not.toHaveBeenCalled();

    await asyncRender(<TabKeywordTimeRange defaultValue="Last hour" />);

    expect(ToolsStore.testNaturalDate).toHaveBeenCalledWith('Last hour');
  });

  it('sets validation state to error if initial value is empty', async () => {
    const { container } = render(<TabKeywordTimeRange defaultValue=" " />);

    await waitFor(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('sets validation state to error if parsing fails initially', async () => {
    ToolsStore.testNaturalDate = () => Promise.reject();

    const { container } = render(<TabKeywordTimeRange defaultValue="invalid" />);

    await waitFor(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('sets validation state to error if parsing fails after changing input', async () => {
    ToolsStore.testNaturalDate = () => Promise.reject();

    const { container, getByDisplayValue } = render(<TabKeywordTimeRange defaultValue="last week" />);
    const input = getByDisplayValue('last week');

    await changeInput(input, 'invalid');

    await waitFor(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('resets validation state if parsing succeeds after changing input', async () => {
    const { container, getByDisplayValue } = render(<TabKeywordTimeRange defaultValue="last week" />);
    const input = getByDisplayValue('last week');

    await changeInput(input, 'last hour');

    await waitFor(() => expect(findValidationState(container)).toEqual(null));
  });

  it('does not show keyword preview if parsing fails', async () => {
    ToolsStore.testNaturalDate = () => Promise.reject();
    const { queryByText } = await asyncRender(<TabKeywordTimeRange defaultValue="invalid" />);

    expect(queryByText('Preview:')).toBeNull();
  });

  it('does not show keyword preview if parsing fails after changing input', async () => {
    const { getByDisplayValue, queryByText } = await asyncRender(<TabKeywordTimeRange defaultValue="last week" />);

    asMock(ToolsStore.testNaturalDate).mockImplementation(() => Promise.reject());
    const input = getByDisplayValue('last week');

    await changeInput(input, 'invalid');

    expect(queryByText('Preview:')).toBeNull();
  });

  it('shows error message if parsing fails after changing input', async () => {
    const { getByDisplayValue, queryByText } = await asyncRender(<TabKeywordTimeRange defaultValue="last week" />);

    asMock(ToolsStore.testNaturalDate).mockImplementation(() => Promise.reject());
    const input = getByDisplayValue('last week');

    await changeInput(input, 'invalid');

    expect(queryByText('Unable to parse keyword.')).not.toBeNull();
  });
});
