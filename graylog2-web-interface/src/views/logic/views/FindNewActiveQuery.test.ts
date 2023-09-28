import * as Immutable from 'immutable';

import FindNewActiveQuery from 'views/logic/views/FindNewActiveQuery';

describe('FindNewActiveQuery', () => {
  it('does not break when there are no queries left', () => {
    expect(FindNewActiveQuery(Immutable.List(), 'deadbeef')).toBeUndefined();
    expect(FindNewActiveQuery(Immutable.List(['foo']), 'deadbeef', Immutable.List(['foo']))).toBeUndefined();
  });

  it('returns current query when it is still present', () => {
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'foo', Immutable.List(['bar']))).toEqual('foo');
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'foo', Immutable.List(['bar', 'baz']))).toEqual('foo');
  });

  it('returns next query when current query is removed', () => {
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'bar', Immutable.List(['bar']))).toEqual('foo');
    expect(FindNewActiveQuery(Immutable.List(['foo', 'bar', 'baz']), 'foo', Immutable.List(['foo', 'baz']))).toEqual('bar');
  });
});
