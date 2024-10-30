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

import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import HighlightingRulesProvider from 'views/components/contexts/HighlightingRulesProvider';

import OriginalHighlightingRules from './HighlightingRules';

const HighlightingRules = ({ rules = [] }: { rules?: Array<HighlightingRule> }) => {
  const formatting = FormattingSettings.create(rules);
  const defaultView = createSearch();
  const view = defaultView
    .toBuilder()
    .state(defaultView.state.update('query-id-1', (viewState) => viewState.toBuilder().formatting(formatting).build()))
    .build();

  return (
    <TestStoreProvider view={view}>
      <HighlightingRulesProvider>
        <OriginalHighlightingRules />
      </HighlightingRulesProvider>
    </TestStoreProvider>
  );
};

describe('HighlightingRules', () => {
  useViewsPlugin();

  it('renders search term legend even when rules are empty', async () => {
    render(<HighlightingRules />);

    await screen.findByText('Search terms');
    const colorPreview = await screen.findByTestId('static-color-preview');

    expect(colorPreview).toHaveStyleRule('background-color', '#ffec3d');
  });

  it('renders element for each HighlightingRule', async () => {
    const rules = [
      HighlightingRule.create('foo', 'bar', undefined, StaticColor.create('#f4f141')),
      HighlightingRule.create('response_time', '250', undefined, StaticColor.create('#f44242')),
    ];
    render(<HighlightingRules rules={rules} />);

    const highlightingRules = await screen.findAllByTestId('highlighting-rule');

    expect(highlightingRules[0].textContent).toMatch(/foo == "bar"/);
    expect(highlightingRules[1].textContent).toMatch(/response_time == "250"/);
  });
});
