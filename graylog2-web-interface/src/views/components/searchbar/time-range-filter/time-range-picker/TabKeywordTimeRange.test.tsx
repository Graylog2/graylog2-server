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
import { asElement, fireEvent, render, waitFor, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';
import { act } from 'react';

import asMock from 'helpers/mocking/AsMock';
import ToolsStore from 'stores/tools/ToolsStore';
import { EMPTY_RANGE } from 'views/components/searchbar/time-range-filter/TimeRangeDisplay';

import OriginalTabKeywordTimeRange from './TabKeywordTimeRange';

jest.mock('stores/tools/ToolsStore', () => ({
  testNaturalDate: jest.fn(),
}));

jest.mock('views/logic/debounceWithPromise', () => (fn: any) => fn);

const TabKeywordTimeRange = ({ keyword, ...props }: { keyword: string } & React.ComponentProps<typeof TabKeywordTimeRange>) => (
  <Formik initialValues={{ timeRangeTabs: { keyword: { type: 'keyword', keyword } }, activeTab: 'keyword' }}
          onSubmit={() => {}}
          validate={(values) => (values.timeRangeTabs.keyword.keyword === 'invalid'
            ? { timeRangeTabs: { keyword: { keyword: 'validation error' } } }
            : {})}
          validateOnMount>
    <Form>
      <OriginalTabKeywordTimeRange {...props as React.ComponentProps<typeof TabKeywordTimeRange>} />
    </Form>
  </Formik>
);

describe('TabKeywordTimeRange', () => {
  beforeEach(() => {
    asMock(ToolsStore.testNaturalDate).mockImplementation(() => Promise.resolve({
      type: 'absolute',
      from: '2018-11-14 13:52:38',
      to: '2018-11-14 13:57:38',
      timezone: 'Europe/Berlin',
    }));

    asMock(ToolsStore.testNaturalDate).mockClear();
  });

  const findValidationState = (container) => {
    const formGroup = container.querySelector('.form-group');

    return formGroup && formGroup.className.includes('has-error')
      ? 'error'
      : null;
  };

  // eslint-disable-next-line testing-library/no-unnecessary-act
  const changeInput = async (input, value) => act(async () => {
    const { name } = asElement(input, HTMLInputElement);

    fireEvent.change(input, { target: { value, name } });
  });

  const asyncRender = async (element) => {
    let wrapper;

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => { wrapper = render(element); });

    if (!wrapper) {
      throw new Error('Render returned `null`.');
    }

    return wrapper;
  };

  it('renders value passed to it', async () => {
    await asyncRender(<TabKeywordTimeRange keyword="Last hour" />);

    await screen.findByDisplayValue('Last hour');
  });

  it('calls onChange if value changes', async () => {
    const { getByDisplayValue } = await asyncRender(<TabKeywordTimeRange keyword="Last hour" />);
    const input = getByDisplayValue('Last hour');

    await changeInput(input, 'last year');

    expect(input).toHaveValue('last year');
  });

  it('calls testNaturalDate', async () => {
    expect(ToolsStore.testNaturalDate).not.toHaveBeenCalled();

    await asyncRender(<TabKeywordTimeRange keyword="Last hour" />);

    expect(ToolsStore.testNaturalDate).toHaveBeenCalledWith('Last hour', 'Europe/Berlin');
  });

  it('does not call testNaturalDate when keyword is empty', async () => {
    expect(ToolsStore.testNaturalDate).not.toHaveBeenCalled();

    await asyncRender(<TabKeywordTimeRange keyword="   " />);

    expect(ToolsStore.testNaturalDate).not.toHaveBeenCalled();
  });

  it('shows validation errors', async () => {
    asMock(ToolsStore.testNaturalDate).mockImplementation(() => Promise.reject());

    const { container } = render(<TabKeywordTimeRange keyword="invalid" />);

    await waitFor(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('does not show keyword preview if parsing fails', async () => {
    asMock(ToolsStore.testNaturalDate).mockImplementation(() => Promise.reject());
    await asyncRender(<TabKeywordTimeRange keyword="invalid" />);

    await waitFor(() => expect(screen.getAllByText(EMPTY_RANGE).length).toEqual(2));
  });
});
