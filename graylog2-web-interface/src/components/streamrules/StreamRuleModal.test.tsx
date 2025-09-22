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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import selectEvent from 'helpers/selectEvent';
import { MockStore, asMock } from 'helpers/mocking';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import { streamRuleTypes } from 'fixtures/streamRuleTypes';

import StreamRuleModal from './StreamRuleModal';

jest.mock('components/streams/hooks/useStreamRuleTypes');
jest.mock('@graylog/server-api', () => ({
  SystemFields: {
    fields: async () => ({ fields: [] }),
  },
}));

jest.mock('stores/inputs/StreamRulesInputsStore', () => ({
  StreamRulesInputsActions: {
    list: jest.fn(),
  },
  StreamRulesInputsStore: MockStore([
    'getInitialState',
    () => ({
      inputs: [{ id: 'my-id', title: 'input title', name: 'name' }],
    }),
  ]),
}));

describe('StreamRuleModal', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof StreamRuleModal>>) => (
    <StreamRuleModal
      onSubmit={() => Promise.resolve()}
      onClose={() => {}}
      submitButtonText="Update rule"
      submitLoadingText="Updating rule..."
      title="Bach"
      {...props}
    />
  );

  const getStreamRule = (type = 1) => ({
    id: 'dead-beef',
    type,
    field: 'field_1',
    value: 'value_1',
    inverted: false,
    description: 'description',
  });

  beforeEach(() => {
    asMock(useStreamRuleTypes).mockReturnValue({ data: streamRuleTypes });
  });

  it('should render without provided stream rule', async () => {
    render(<SUT />);

    await screen.findByRole('combobox', {
      name: /field/i,
    });
  });

  it('should render with provided stream rule', async () => {
    render(<SUT initialValues={getStreamRule()} />);

    await screen.findByRole('combobox', { name: /field/i });

    const valueInput = await screen.findByRole('textbox', {
      name: /value/i,
    });

    expect(await screen.findAllByText('field_1')).toHaveLength(2);
    expect(valueInput).toHaveValue('value_1');
  });

  it('should require selected input when type is `match input`', async () => {
    const submit = jest.fn(() => Promise.resolve());

    render(<SUT onSubmit={submit} initialValues={getStreamRule()} />);

    const submitBtn = await screen.findByRole('button', {
      name: /update rule/i,
    });

    await selectEvent.chooseOption('Type', 'match input');

    expect(submitBtn).toBeDisabled();

    await selectEvent.chooseOption('Input', 'input title (name)');

    await waitFor(() => expect(submitBtn).toBeEnabled());
    userEvent.click(submitBtn);

    await waitFor(() => expect(submit).toHaveBeenCalledTimes(1));

    expect(submit).toHaveBeenCalledWith('dead-beef', {
      description: 'description',
      id: 'dead-beef',
      inverted: false,
      field: '',
      type: 8,
      value: 'my-id',
    });
  });
});
