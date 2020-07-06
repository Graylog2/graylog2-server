// @flow strict
import * as Immutable from 'immutable';

import queryTitle from './QueryTitle';
import Query from './Query';

import Search from '../search/Search';
import View from '../views/View';
import ViewState from '../views/ViewState';

describe('QueryTitle', () => {
  const search = Search.create().toBuilder().queries([
    Query.builder().id('foo').build(),
    Query.builder().id('bar').build(),
    Query.builder().id('baz').build(),
  ]).build();
  const view = View.create().toBuilder().search(search)
    .state(Immutable.fromJS({
      foo: ViewState.builder().titles(Immutable.fromJS({
        tab: { title: 'The Fabulous Foo Tab' },
      })).build(),
      baz: ViewState.builder().titles(Immutable.fromJS({
        tab: { title: 'The Incredible Other Tab' },
      })).build(),
    }))
    .build();

  it('returns actual name of first tab', () => {
    expect(queryTitle(view, 'foo')).toEqual('The Fabulous Foo Tab');
  });

  it('returns generated name of nameless second tab', () => {
    expect(queryTitle(view, 'bar')).toEqual('Page#2');
  });

  it('returns actual name of third tab', () => {
    expect(queryTitle(view, 'baz')).toEqual('The Incredible Other Tab');
  });

  it('returns `undefined` for missing tab', () => {
    expect(queryTitle(view, 'qux')).toEqual(undefined);
  });

  it('returns `undefined` if query id is `undefined`', () => {
    // $FlowFixMe: passing invalid values on purpose
    expect(queryTitle(view, undefined)).toEqual(undefined);
  });

  it('returns `undefined` if view is `undefined`', () => {
    // $FlowFixMe: passing invalid values on purpose
    expect(queryTitle(undefined, undefined)).toEqual(undefined);
  });

  it('returns `undefined` if search is `undefined`', () => {
    // $FlowFixMe: passing invalid values on purpose
    expect(queryTitle(View.create(), undefined)).toEqual(undefined);
  });

  it('returns `undefined` if queries are `undefined`', () => {
    expect(queryTitle(View.create()
      .toBuilder()
      .search(Search.create()
        .toBuilder()
        // $FlowFixMe: passing invalid values on purpose
        .queries(undefined)
        .build())
      // $FlowFixMe: passing invalid values on purpose
      .build(), undefined)).toEqual(undefined);
  });
});
