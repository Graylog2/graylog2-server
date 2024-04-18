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
import { render } from 'wrappedTestingLibrary';

import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import ViewState from 'views/logic/views/ViewState';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import type View from 'views/logic/views/View';
import { createSearch } from 'fixtures/searches';

import HighlightingRulesContext from './HighlightingRulesContext';
import HighlightingRulesProvider from './HighlightingRulesProvider';

describe('HighlightingRulesProvider', () => {
  const renderSUT = (view: View = createSearch()) => {
    const consume = jest.fn();

    render((
      <TestStoreProvider view={view}>
        <HighlightingRulesProvider>
          <HighlightingRulesContext.Consumer>
            {consume}
          </HighlightingRulesContext.Consumer>
        </HighlightingRulesProvider>
      </TestStoreProvider>
    ));

    return consume;
  };

  useViewsPlugin();

  it('provides no data when highlighting rules store is empty', () => {
    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith([]);
  });

  it('provides highlighting rules', () => {
    const rule = HighlightingRule.builder()
      .field('field-name')
      .value(String(42))
      .color(StaticColor.create('#bc98fd'))
      .build();
    const viewState = ViewState.builder()
      .formatting(FormattingSettings.create([rule]))
      .build();
    const view = createSearch()
      .toBuilder()
      .state({ 'query-id-1': viewState })
      .build();

    const consume = renderSUT(view);

    expect(consume).toHaveBeenCalledWith([rule]);
  });
});
