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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import 'helpers/mocking/react-dom_mock';
import userEvent from '@testing-library/user-event';

import GrokPatternInput from 'components/grok-patterns/GrokPatternInput';

describe('<GrokPatternInput />', () => {
  const grokPatterns = [
    { name: 'COMMONMAC', pattern: '(?:(?:[A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2})' },
    { name: 'DATA', pattern: '.*?' },
    { name: 'DATE', pattern: '%{MONTHDAY}[./-]%{MONTHNUM}[./-]%{YEAR}' },
  ];

  it('should render grok pattern input without patterns', async () => {
    render(<GrokPatternInput patterns={[]} />);

    await screen.findByLabelText('Pattern');
  });

  it('should render grok pattern input with patterns', async () => {
    render(<GrokPatternInput patterns={grokPatterns} />);

    await screen.findByText('COMMONMAC');
  });

  it('should add a grok pattern when selected', async () => {
    const changeFn = jest.fn((pattern) => {
      expect(pattern).toEqual('%{COMMONMAC}');
    });
    render(<GrokPatternInput patterns={grokPatterns} onPatternChange={changeFn} />);

    const addButtons = await screen.findAllByRole('button', { name: 'Add' });
    addButtons[0].click();

    expect(changeFn).toHaveBeenCalled();
  });

  it('should filter the grok patterns', async () => {
    render(<GrokPatternInput patterns={grokPatterns} />);

    const addButtons = await screen.findAllByRole('button', { name: 'Add' });

    expect(addButtons).toHaveLength(3);

    const filterInput = await screen.findByRole('textbox', { name: /filter pattern/i });
    userEvent.type(filterInput, 'COMMON');

    await waitFor(async () => {
      const patternButtons = await screen.findAllByRole('button', { name: 'Add' });

      expect(patternButtons).toHaveLength(1);
    });
  });
});
