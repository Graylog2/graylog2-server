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
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import { MockStore, asMock } from 'helpers/mocking';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import { streamRuleTypes } from 'fixtures/streamRuleTypes';

import StreamRuleModal from './StreamRuleModal';

jest.mock('components/streams/hooks/useStreamRuleTypes');

jest.mock('stores/inputs/StreamRulesInputsStore', () => ({
  StreamRulesInputsActions: {
    list: jest.fn(),
  },
  StreamRulesInputsStore: MockStore(['getInitialState', () => ({
    inputs: [
      { id: 'my-id', title: 'input title', name: 'name' },
    ],
  })]),
}));

describe('StreamRuleModal', () => {
  const SUT = (props: Partial<React.ComponentProps<typeof StreamRuleModal>>) => (
    <StreamRuleModal onSubmit={() => Promise.resolve()}
                     onClose={() => {}}
                     submitButtonText="Update rule"
                     submitLoadingText="Updating rule..."
                     title="Bach"
                     {...props} />
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

    await screen.findByRole('textbox', {
      name: /field/i,
      hidden: true,
    });
  });

  it('should render with provided stream rule', async () => {
    render(<SUT initialValues={getStreamRule()} />);

    const fieldInput = await screen.findByRole('textbox', {
      name: /field/i,
      hidden: true,
    });

    const valueInput = await screen.findByRole('textbox', {
      name: /value/i,
      hidden: true,
    });

    expect(fieldInput).toHaveValue('field_1');
    expect(valueInput).toHaveValue('value_1');
  });

  it('should require selected input when type is `match input`', async () => {
    const submit = jest.fn(() => Promise.resolve());

    render(
      <SUT onSubmit={submit}
           initialValues={getStreamRule()} />,
    );

    const submitBtn = await screen.findByRole('button', {
      name: /update rule/i,
      hidden: true,
    });

    const ruleTypeSelect = await screen.findByLabelText('Type');
    selectEvent.openMenu(ruleTypeSelect);
    await selectEvent.select(ruleTypeSelect, 'match input');

    expect(submitBtn).toBeDisabled();

    const inputSelect = await screen.findByLabelText('Input');
    selectEvent.openMenu(inputSelect);
    await selectEvent.select(inputSelect, 'input title (name)');

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
