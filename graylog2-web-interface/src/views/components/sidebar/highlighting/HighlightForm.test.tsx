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
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';

jest.mock('views/stores/HighlightingRulesStore', () => ({
  HighlightingRulesActions: {
    add: jest.fn(() => Promise.resolve()),
    remove: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
  },
}));

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
  const HighlightFormWithContext = (props) => (
    <FieldTypesContext.Provider value={fieldTypes}>
      <HighlightForm {...props} />
    </FieldTypesContext.Provider>
  );

  const triggerSaveButtonClick = async () => {
    const elem = await screen.findByText('Save');
    fireEvent.click(elem);
  };

  it('should render for edit', async () => {
    const { findByText } = render(<HighlightFormWithContext onClose={() => {}} rule={rule} />);

    const form = await findByText('Edit Highlighting Rule');
    const input = await screen.findByLabelText('Value');

    expect(input).toHaveValue(String(rule.value));
    expect(form).toBeInTheDocument();
  });

  it('should render for new', async () => {
    const { findByText } = render(<HighlightFormWithContext onClose={() => {}} />);

    const form = await findByText('New Highlighting Rule');

    expect(form).toBeInTheDocument();
  });

  it('should fire onClose on cancel', async () => {
    const onClose = jest.fn();
    const { findByText } = render(<HighlightFormWithContext onClose={onClose} />);

    const elem = await findByText('Cancel');

    fireEvent.click(elem);

    expect(onClose).toBeCalledTimes(1);
  });

  it('should fire remove action when saving a existing rule', async () => {
    render(<HighlightFormWithContext onClose={() => {}} rule={rule} />);

    await triggerSaveButtonClick();

    await waitFor(() => expect(HighlightingRulesActions.update)
      .toBeCalledWith(rule, { field: rule.field, value: rule.value, condition: rule.condition, color: rule.color }));
  });

  it('assigns a new static color when type is selected', async () => {
    render(<HighlightFormWithContext onClose={() => {}} rule={rule} />);

    userEvent.click(screen.getByLabelText('Static Color'));

    userEvent.click(screen.getByText('Save'));

    await waitFor(() => expect(HighlightingRulesActions.update)
      .toBeCalledWith(rule, expect.objectContaining({
        color: expect.objectContaining({ type: 'static', color: expect.any(String) }),
      })));
  });

  it('creates a new gradient when type is selected', async () => {
    render(<HighlightFormWithContext onClose={() => {}} rule={rule} />);

    userEvent.click(screen.getByLabelText('Gradient'));

    const highestValue = await screen.findByLabelText('Specify highest value');
    userEvent.clear(highestValue);
    userEvent.type(highestValue, '100');

    userEvent.click(screen.getByText('Save'));

    await waitFor(() => expect(HighlightingRulesActions.update)
      .toBeCalledWith(rule, expect.objectContaining({
        color: expect.objectContaining({ gradient: 'Viridis' }),
      })));
  });

  it('should be able to click submit when has value 0 with type number', async () => {
    render(<HighlightFormWithContext onClose={() => {}} rule={ruleWithValueZero} />);

    await triggerSaveButtonClick();

    await waitFor(() => expect(HighlightingRulesActions.update)
      .toBeCalledWith(ruleWithValueZero, { field: ruleWithValueZero.field, value: '0', condition: ruleWithValueZero.condition, color: ruleWithValueZero.color }));
  });

  it('should be able to click submit when has value false  with type boolean', async () => {
    render(<HighlightFormWithContext onClose={() => {}} rule={ruleWithValueFalse} />);

    await triggerSaveButtonClick();

    await waitFor(() => expect(HighlightingRulesActions.update)
      .toBeCalledWith(ruleWithValueFalse, { field: ruleWithValueFalse.field, value: 'false', condition: ruleWithValueFalse.condition, color: ruleWithValueFalse.color }));
  });
});
