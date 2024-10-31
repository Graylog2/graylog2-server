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
import { render, screen } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import { AdditionalContext } from 'views/logic/ActionContext';

import SelectExtractorType from './SelectExtractorType';

import FieldType from '../fieldtypes/FieldType';

describe('SelectExtractorType', () => {
  const value = 'value of message';
  const field = 'value_field';
  const focus = jest.fn();

  // @ts-ignore
  window.open = jest.fn(() => ({ focus }));

  const message = {
    fields: {
      gl2_source_input: 'input-id',
      gl2_source_node: 'node-id',
    },
    formattedFields: {},
    id: 'message-id',
    index: 'message-index',
  };

  it('should render', async () => {
    render(
      <AdditionalContext.Provider key="message-key" value={{ message }}>
        <SelectExtractorType onClose={() => {}} value={value} field={field} queryId="foo" type={FieldType.Unknown} />
      </AdditionalContext.Provider>,
    );

    await screen.findByRole('heading', { name: /select extractor type/i, hidden: true });
  });

  it('should select a extractor and open a new window', async () => {
    render(
      <AdditionalContext.Provider key="message-key" value={{ message }}>
        <SelectExtractorType onClose={() => {}} value={value} field={field} queryId="foo" type={FieldType.Unknown} />
      </AdditionalContext.Provider>,
    );

    const extractorType = (await screen.findAllByText(/select extractor type/i))[1];
    await selectEvent.openMenu(extractorType);
    await selectEvent.select(extractorType, 'Grok pattern');

    await userEvent.click(await screen.findByRole('button', { name: /submit/i, hidden: true }));

    expect(window.open).toHaveBeenCalled();
    expect(focus).toHaveBeenCalled();
  });
});
