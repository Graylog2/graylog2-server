// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import EmptyAggregationContent from './EmptyAggregationContent';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

describe('EmptyAggregationContext', () => {
  it('calls render completion callback after first render', () => {
    const onRenderComplete = jest.fn();
    mount((
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <EmptyAggregationContent toggleEdit={() => {}} editing={false} />
      </RenderCompletionCallback.Provider>
    ));

    expect(onRenderComplete).toHaveBeenCalled();
  });
});
