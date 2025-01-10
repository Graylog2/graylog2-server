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
import { render, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import * as Immutable from 'immutable';

import HighlightForm from 'views/components/sidebar/highlighting/HighlightForm';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';

const rule = HighlightingRule.builder()
  .color(StaticColor.create('#333333'))
  .condition('not_equal')
  .field('foob')
  .value('noob')
  .build();

const ruleWithValueFalse = rule.toBuilder()
  .value(false)
  .build();
const ruleWithValueZero = rule.toBuilder()
  .value(0)
  .build();

describe('HighlightForm', () => {
  const fieldTypes: FieldTypes = {
    all: Immutable.List([FieldTypeMapping.create('foob', FieldType.create('long', [Properties.Numeric]))]),
    queryFields: Immutable.Map(),
  };
  const SUT = (props: Partial<React.ComponentProps<typeof HighlightForm>>) => (
    <TestStoreProvider>
      <FieldTypesContext.Provider value={fieldTypes}>
        <HighlightForm onClose={() => {}} rule={undefined} onSubmit={() => Promise.resolve()} {...props} />
      </FieldTypesContext.Provider>
    </TestStoreProvider>
  );

  const triggerSaveButtonClick = async () => {
    const elem = await screen.findByText('Update rule');
    fireEvent.click(elem);
  };

  useViewsPlugin();

  it('should render for edit', async () => {
    const { findByText } = render(<SUT rule={rule} />);

    const form = await findByText('Edit Highlighting Rule');
    const input = await screen.findByLabelText('Value');

    expect(input).toHaveValue(String(rule.value));
    expect(form).toBeInTheDocument();
  });

  it('should render for new', async () => {
    const { findByText } = render(<SUT rule={undefined} />);

    await findByText('Create Highlighting Rule');
  });

  it('should fire onClose on cancel', async () => {
    const onClose = jest.fn();
    const { findByText } = render(<SUT onClose={onClose} />);
    const elem = await findByText('Cancel');

    fireEvent.click(elem);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should fire update action when saving a existing rule', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    render(<SUT rule={rule} onSubmit={onSubmit} />);

    await triggerSaveButtonClick();

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(rule.field, rule.value, rule.condition, rule.color));
  });

  it('assigns a new static color when type is selected', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    render(<SUT rule={rule} onSubmit={onSubmit} />);

    userEvent.click(screen.getByLabelText('Static Color'));

    await triggerSaveButtonClick();

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(rule.field, rule.value, rule.condition, expect.objectContaining({ type: 'static', color: expect.any(String) })));
  });

  it('creates a new gradient when type is selected', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    render(<SUT rule={rule} onSubmit={onSubmit} />);

    userEvent.click(screen.getByLabelText('Gradient'));

    const highestValue = await screen.findByLabelText('Specify highest value');
    userEvent.clear(highestValue);
    userEvent.type(highestValue, '100');

    await triggerSaveButtonClick();

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(rule.field, rule.value, rule.condition, expect.objectContaining({ gradient: 'Viridis' })));
  });

  it('should be able to click submit when has value 0 with type number', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    render(<SUT rule={ruleWithValueZero} onSubmit={onSubmit} />);

    await triggerSaveButtonClick();
    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(ruleWithValueZero.field, '0', ruleWithValueZero.condition, ruleWithValueZero.color));
  });

  it('should be able to click submit when has value false with type boolean', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    render(<SUT rule={ruleWithValueFalse} onSubmit={onSubmit} />);

    await triggerSaveButtonClick();

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(ruleWithValueFalse.field, 'false', ruleWithValueFalse.condition, ruleWithValueFalse.color));
  });
});
