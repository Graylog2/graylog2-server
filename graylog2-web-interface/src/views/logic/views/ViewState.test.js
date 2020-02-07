// @flow strict
import ViewState from './ViewState';

describe('ViewState', () => {
  it('duplicates empty view state', () => {
    const viewState = ViewState.create();
    const viewState2 = viewState.duplicate();

    expect(viewState2).toBeInstanceOf(ViewState);
  });
});
