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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import HighlightForm from 'views/components/sidebar/highlighting/HighlightForm';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';

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

describe('HighlightForm', () => {
  it('should render for edit', async () => {
    const { findByText, findByDisplayValue } = render(<HighlightForm onClose={() => {}} rule={rule} />);

    const form = await findByText('Edit Highlighting Rule');
    const value = await findByDisplayValue(rule.value);

    expect(form).toBeInTheDocument();
    expect(value).toBeInTheDocument();
  });

  it('should render for new', async () => {
    const { findByText } = render(<HighlightForm onClose={() => {}} />);

    const form = await findByText('New Highlighting Rule');

    expect(form).toBeInTheDocument();
  });

  it('should fire onClose on cancel', async () => {
    const onClose = jest.fn();
    const { findByText } = render(<HighlightForm onClose={onClose} />);

    const elem = await findByText('Cancel');

    fireEvent.click(elem);

    expect(onClose).toBeCalledTimes(1);
  });

  it('should fire remove action when saving a existing rule', async () => {
    const { findByText } = render(<HighlightForm onClose={() => {}} rule={rule} />);

    const elem = await findByText('Save');

    fireEvent.click(elem);

    await waitFor(() => expect(HighlightingRulesActions.update)
      .toBeCalledWith(rule, { field: rule.field, value: rule.value, condition: rule.condition, color: rule.color }));
  });
});
