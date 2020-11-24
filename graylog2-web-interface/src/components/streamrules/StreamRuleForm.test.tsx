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
import * as React from 'react';
import { cleanup, render, fireEvent } from 'wrappedTestingLibrary';

import StreamRuleForm from './StreamRuleForm';

jest.mock('components/common', () => ({
  // eslint-disable-next-line react/prop-types
  TypeAheadFieldInput: ({ defaultValue }) => (<div>{defaultValue}</div>),
  // eslint-disable-next-line react/prop-types
  Icon: ({ children }) => (<div>{children}</div>),
}));

describe('StreamRuleForm', () => {
  afterEach(() => {
    cleanup();
  });

  const streamRuleTypes = [
    { id: 1, short_desc: 'match exactly', long_desc: 'match exactly' },
    { id: 2, short_desc: 'match regular expression', long_desc: 'match regular expression' },
    { id: 3, short_desc: 'greater than', long_desc: 'greater than' },
    { id: 4, short_desc: 'smaller than', long_desc: 'smaller than' },
    { id: 5, short_desc: 'field presence', long_desc: 'field presence' },
    { id: 6, short_desc: 'contain', long_desc: 'contain' },
    { id: 7, short_desc: 'always match', long_desc: 'always match' },
    { id: 8, short_desc: 'match input', long_desc: 'match input' },
  ];

  const getStreamRule = (type = 1) => {
    return {
      id: 'dead-beef',
      type,
      field: 'field_1',
      value: 'value_1',
      inverted: false,
      description: 'description',
    };
  };

  it('should render an empty StreamRuleForm', () => {
    const container = render(
      <StreamRuleForm onSubmit={() => {}}
                      streamRuleTypes={streamRuleTypes}
                      title="Bach" />,
    );

    expect(container).not.toBeNull();
  });

  it('should render an simple StreamRuleForm', () => {
    const container = render(
      <StreamRuleForm onSubmit={() => {}}
                      streamRule={getStreamRule()}
                      streamRuleTypes={streamRuleTypes}
                      title="Bach" />,
    );

    expect(container).not.toBeNull();
  });

  it('should validate the selection of match input', () => {
    const submit = jest.fn();
    const inputs = [
      { id: 'my-id', title: 'title', name: 'name' },
    ];
    const { getByTestId, getByText } = render(
      <StreamRuleForm onSubmit={submit}
                      streamRule={getStreamRule()}
                      inputs={inputs}
                      streamRuleTypes={streamRuleTypes}
                      title="Bach" />,
    );

    const ruleTypeSelection = getByTestId('rule-type-selection');
    fireEvent.change(ruleTypeSelection, { target: { name: 'type', value: 8 } });
    const submitBtn = getByText('Save');
    fireEvent.click(submitBtn);

    expect(submit).toHaveBeenCalledTimes(0);

    const inputSelection = getByTestId('input-selection');
    fireEvent.change(inputSelection, { target: { name: 'value', value: 'my-id' } });
    fireEvent.click(submitBtn);

    expect(submit).toHaveBeenCalledTimes(1);
  });
});
