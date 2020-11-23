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
                                   updateConfig={() => {}}
                                   handleFormEvent={() => {}}
                                   validationState={() => 'success'}
                                   validationMessage={() => ''} />,
    );

    expect(container).not.toBeNull();
  });

  it('should add a header', () => {
    const updateConfig = jest.fn();
    const { getByTestId, getByText } = render(
      <HTTPJSONPathAdapterFieldSet config={config}
                                   updateConfig={updateConfig}
                                   handleFormEvent={() => {}}
                                   validationState={() => 'success'}
                                   validationMessage={() => ''} />,
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
