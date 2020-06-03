// @flow strict
import * as React from 'react';
import { cleanup, render } from 'wrappedTestingLibrary';

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
  afterEach(cleanup);

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
