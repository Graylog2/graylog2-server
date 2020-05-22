// @flow strict
import React from 'react';
import { fireEvent, render } from 'wrappedTestingLibrary';
import HTTPJSONPathAdapterFieldSet from './HTTPJSONPathAdapterFieldSet';

describe('HTTPJSONPathAdapterFieldSet', () => {
  const config = {
    url: 'https://example.org/test',
    headers: { test: 'headers' },
    single_value_jsonpath: 'foo.bar',
    multi_value_jsonpath: 'bar.foo',
    user_agent: 'smith',
  };

  it('should render the field set', () => {
    const { container } = render(
      <HTTPJSONPathAdapterFieldSet config={config}
                                   updateConfig={() => {
                                   }}
                                   handleFormEvent={() => {
                                   }}
                                   validationState={() => {
                                   }}
                                   validationMessage={() => {
                                   }} />,
    );
    expect(container).toMatchSnapshot();
  });

  it('should add a header', () => {
    const updateConfig = jest.fn();
    const { getByTestId, getByText } = render(
      <HTTPJSONPathAdapterFieldSet config={config}
                                   updateConfig={updateConfig}
                                   handleFormEvent={() => {
                                   }}
                                   validationState={() => {
                                   }}
                                   validationMessage={() => {
                                   }} />,
    );
    const newKeyInput = getByTestId('newKey');
    const newValueInput = getByTestId('newValue');
    const addBtn = getByText('Add');

    fireEvent.change(newKeyInput, { target: { value: 'new Key' } });
    fireEvent.change(newValueInput, { target: { value: 'new Value' } });
    fireEvent.click(addBtn);

    const newConfig = {
      ...config,
      headers: {
        ...config.headers,
        'new Key': 'new Value',
      },
    };
    expect(updateConfig).toBeCalledWith(newConfig);
  });
});
