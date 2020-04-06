// @flow strict
import React from 'react';
import { fireEvent, render, wait } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import ToolsStore from 'stores/tools/ToolsStore';

import OriginalKeywordTimeRangeSelector from './KeywordTimeRangeSelector';

jest.mock('stores/tools/ToolsStore', () => ({}));

const KeywordTimeRangeSelector = ({ value, ...props }: { value: string }) => (
  <Formik initialValues={{ timerange: { type: 'keyword', keyword: value } }} validateOnMount>
    <Form>
      <OriginalKeywordTimeRangeSelector {...props} />
    </Form>
  </Formik>
);

jest.mock('logic/datetimes/DateTime', () => ({ fromUTCDateTime: (date) => date }));

describe('KeywordTimeRangeSelector', () => {
  beforeEach(() => {
    ToolsStore.testNaturalDate = jest.fn(() => Promise.resolve({
      from: '2018-11-14 13:52:38',
      to: '2018-11-14 13:57:38',
    }));
  });

  const findValidationState = (container) => {
    return container.querySelector('.form-group').className.includes('has-error')
      ? 'error'
      : null;
  };

  it('renders value passed to it', () => {
    const { getByDisplayValue } = render(<KeywordTimeRangeSelector value="Last hour" />);
    expect(getByDisplayValue('Last hour')).not.toBeNull();
  });

  it('calls onChange if value changes', () => {
    const { getByDisplayValue } = render(<KeywordTimeRangeSelector value="Last hour" />);
    const input = getByDisplayValue('Last hour');

    fireEvent.change(input, { target: { value: 'last year', name: 'timerange.keyword' } });
  });

  it('calls testNaturalDate', () => {
    expect(ToolsStore.testNaturalDate).not.toHaveBeenCalled();

    render(<KeywordTimeRangeSelector value="Last hour" />);

    expect(ToolsStore.testNaturalDate).toHaveBeenCalledWith('Last hour');
  });

  it('sets validation state to error if initial value is empty', async () => {
    const { container } = render(<KeywordTimeRangeSelector value="" />);

    await wait(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('sets validation state to error if parsing fails initially', async () => {
    ToolsStore.testNaturalDate = () => Promise.reject();

    const { container } = render(<KeywordTimeRangeSelector value="invalid" />);

    await wait(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('sets validation state to error if parsing fails after changing input', async () => {
    ToolsStore.testNaturalDate = () => Promise.reject();

    const { container, getByDisplayValue } = render(<KeywordTimeRangeSelector value="last week" />);
    const input = getByDisplayValue('last week');

    fireEvent.change(input, { target: { value: 'invalid', name: 'timerange.keyword' } });

    await wait(() => expect(findValidationState(container)).toEqual('error'));
  });

  it('resets validation state if parsing succeeds after changing input', async () => {
    const { container, getByDisplayValue } = render(<KeywordTimeRangeSelector value="last week" />);

    const input = getByDisplayValue('last week');
    fireEvent.change(input, { target: { value: 'last hour', name: 'timerange.keyword' } });

    await wait(() => expect(findValidationState(container)).toEqual(null));
  });

  it('shows keyword preview if parsing succeeded', async () => {
    const { queryByText } = render(<KeywordTimeRangeSelector value="last five minutes" />);

    await wait(() => expect(queryByText('2018-11-14 13:52:38 to 2018-11-14 13:57:38')).not.toBeNull());
  });

  it('does not show keyword preview if parsing fails', () => {
    ToolsStore.testNaturalDate = () => Promise.reject();
    const { queryByText } = render(<KeywordTimeRangeSelector value="invalid" />);

    expect(queryByText('Preview:')).toBeNull();
  });

  it('shows keyword preview if parsing succeeded after changing input', async () => {
    const { getByDisplayValue, queryByText } = render(<KeywordTimeRangeSelector value="" />);

    const input = getByDisplayValue('');
    fireEvent.change(input, { target: { value: 'last hour', name: 'timerange.keyword' } });

    await wait(() => expect(queryByText('2018-11-14 13:52:38 to 2018-11-14 13:57:38')).not.toBeNull());
  });

  it('does not show keyword preview if parsing fails after changing input', () => {
    const { getByDisplayValue, queryByText } = render(<KeywordTimeRangeSelector value="last week" />);

    ToolsStore.testNaturalDate = () => Promise.reject();
    const input = getByDisplayValue('last week');

    fireEvent.change(input, { target: { value: 'invalid', name: 'timerange.keyword' } });

    expect(queryByText('Preview:')).toBeNull();
  });
});
