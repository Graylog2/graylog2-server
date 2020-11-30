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
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';

import HighlightingRulesContext from './HighlightingRulesContext';
import HighlightingRulesProvider from './HighlightingRulesProvider';

jest.mock('views/stores/HighlightingRulesStore', () => ({
  HighlightingRulesStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(() => {}),
  },
}));

describe('HighlightingRulesProvider', () => {
  const renderSUT = () => {
    const consume = jest.fn();

    render(
      <HighlightingRulesProvider>
        <HighlightingRulesContext.Consumer>
          {consume}
        </HighlightingRulesContext.Consumer>
      </HighlightingRulesProvider>,
    );

    return consume;
  };

  it('provides no data when highlighting rules store is empty', () => {
    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith(undefined);
  });

  it('provides highlighting rules', () => {
    const rule = HighlightingRule.builder()
      .field('field-name')
      .value(String(42))
      .color('#bc98fd')
      .build();

    asMock(HighlightingRulesStore.getInitialState).mockReturnValue([rule]);

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith([rule]);
  });
});
