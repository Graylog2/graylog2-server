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
import { render, screen, within } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import useFeature from 'hooks/useFeature';
import usePluginEntities from 'hooks/usePluginEntities';
import FieldTypeValueRenderer from 'views/components/fieldtypes/FieldTypeValueRenderer';
import { useInputs } from 'hooks/useInputs';
import FieldType from 'views/logic/fieldtypes/FieldType';
import { AdditionalContext } from 'views/logic/ActionContext';
import DecoratedValue from 'views/components/messagelist/decoration/DecoratedValue';

jest.mock('hooks/useFeature');
jest.mock('hooks/usePluginEntities');
jest.mock('hooks/useInputs');

describe('TypeSpecificValue', () => {
  beforeEach(() => {
    asMock(useFeature).mockReturnValue(true);

    asMock(usePluginEntities).mockImplementation(
      (entityKey) => ({ fieldTypeValueRenderer: FieldTypeValueRenderer, forwarder: [] })[entityKey],
    );
  });

  it('should render prettified value if unit defined', async () => {
    render(
      <TypeSpecificValue
        field="field1"
        value={6543.21}
        unit={FieldUnit.fromJSON({ abbrev: 'ms', unit_type: 'time' })}
      />,
    );

    await screen.findByText('6.5 s');
  });

  it('should render decorated link with its URL as text', async () => {
    render(
      <TypeSpecificValue
        field="field1"
        value={{ type: 'a', href: 'https://ipam.example.com/search?ip=10.0.0.1' }}
        type={FieldType.Decorated}
      />,
    );

    const link = await screen.findByRole('link', { name: 'https://ipam.example.com/search?ip=10.0.0.1' });

    expect(link).toHaveAttribute('href', 'https://ipam.example.com/search?ip=10.0.0.1');
  });

  it('should render decorated link with custom link text and URL as tooltip', async () => {
    render(
      <TypeSpecificValue
        field="field1"
        value={{ type: 'a', href: 'https://ipam.example.com/search?ip=10.0.0.1', text: 'Open in IPAM' }}
        type={FieldType.Decorated}
      />,
    );

    const link = await screen.findByTitle('https://ipam.example.com/search?ip=10.0.0.1');

    expect(link).toHaveTextContent('Open in IPAM');
    expect(link).toHaveAttribute('href', 'https://ipam.example.com/search?ip=10.0.0.1');
  });

  describe('highlighting of decorated links', () => {
    const originalValue = '10.0.0.1';
    const message = (after: unknown) => ({
      id: 'message-id',
      index: 'graylog_0',
      fields: { field1: after },
      highlight_ranges: { field1: [{ start: 0, length: 4 }] },
      decoration_stats: {
        added_fields: {},
        changed_fields: { field1: { before: originalValue, after } },
        removed_fields: {},
      },
    });

    const renderInMessage = (value: unknown) =>
      render(
        <AdditionalContext.Provider value={{ message: message(value) }}>
          <TypeSpecificValue field="field1" value={value} type={FieldType.Decorated} render={DecoratedValue} />
        </AdditionalContext.Provider>,
      );

    it('highlights link label when it is the original field value', async () => {
      renderInMessage({ type: 'a', href: originalValue });

      const link = await screen.findByRole('link');

      expect(within(link).getByText('10.0')).toHaveStyle({ padding: '0 1px' });
    });

    it('does not apply highlight ranges of original value to custom link text', async () => {
      renderInMessage({ type: 'a', href: 'https://ipam.example.com/search?ip=10.0.0.1', text: 'Open in IPAM' });

      const link = await screen.findByRole('link');

      expect(link).toHaveTextContent('Open in IPAM');
      expect(within(link).queryByText('Open')).not.toBeInTheDocument();
    });

    it('does not apply highlight ranges of original value to templated URL', async () => {
      const href = 'https://ipam.example.com/search?ip=10.0.0.1';
      renderInMessage({ type: 'a', href });

      const link = await screen.findByRole('link');

      expect(link).toHaveTextContent(href);
      expect(within(link).queryByText('http')).not.toBeInTheDocument();
    });
  });

  it('should render original value if unit not defined', async () => {
    render(<TypeSpecificValue field="field1" value={6543.21} />);

    await screen.findByText(6543.21);
  });

  it('should render input titles for input field', async () => {
    asMock(useInputs).mockReturnValue({
      'input-id-1': {
        id: 'input-id-1',
        title: 'Input 1',
        type: 'type',
        global: false,
        name: 'inputName',
        created_at: '',
        creator_user_id: 'creatorId',
        static_fields: {},
        attributes: {},
      },
    });
    render(<TypeSpecificValue field="gl2_source_input" value="input-id-1" type={new FieldType('input', [], [])} />);

    await screen.findByText('Input 1');
  });

  // Regression: error processing stream's message body details may contain BigInt values
  // (deserialized by json-with-bigint), which native JSON.stringify cannot serialize.
  it('should render object values containing BigInts without crashing', async () => {
    const value = {
      message: 'failed to process',
      size: BigInt('9223372036854775807'),
    };

    render(<TypeSpecificValue field="gl2_processing_error" value={value} />);

    await screen.findByText(/9223372036854775807/);
  });
});
