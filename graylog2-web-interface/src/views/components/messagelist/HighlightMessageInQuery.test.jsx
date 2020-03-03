// @flow strict
import * as React from 'react';
import { render } from '@testing-library/react';

import HighlightMessageInQuery from './HighlightMessageInQuery';
import HighlightMessageContext from '../contexts/HighlightMessageContext';

describe('HighlightMessageInQuery', () => {
  const TestComponent = () => (
    <HighlightMessageContext.Consumer>
      {messageId => <span>{messageId}</span>}
    </HighlightMessageContext.Consumer>
  );
  it('should render component for empty query', () => {
    const { container } = render((
      <HighlightMessageInQuery query={undefined}>
        <TestComponent />
      </HighlightMessageInQuery>
    ));
    expect(container).not.toBeNull();
  });
  it('should render component for query without message id', () => {
    const { container } = render((
      <HighlightMessageInQuery query={{}}>
        <TestComponent />
      </HighlightMessageInQuery>
    ));
    expect(container).not.toBeNull();
  });
  it('should pass message id from query to children', () => {
    const { getByText } = render((
      <HighlightMessageInQuery query={{ highlightMessage: 'foobar' }}>
        <TestComponent />
      </HighlightMessageInQuery>
    ));
    expect(getByText('foobar')).not.toBeNull();
  });
});
