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
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import SearchLink from './SearchLink';

const urlPrefix = '/search';

describe('SearchLink', () => {
  it('renders the search route only with no parameters', () => {
    const result = SearchLink.builder().build().toURL();

    expect(result).toEqual(urlPrefix);
  });

  it('renders the search id', () => {
    const result = SearchLink.builder().id('somesearchid').build().toURL();

    expect(result).toEqual(`${urlPrefix}/somesearchid`);
  });

  it('includes the query string', () => {
    const result = SearchLink.builder()
      .query(createElasticsearchQueryString('foo:bar'))
      .build()
      .toURL();

    expect(result).toEqual(`${urlPrefix}?q=foo%3Abar`);
  });

  it('includes the time range', () => {
    const result = SearchLink.builder()
      .timerange({ type: 'relative', from: 300 })
      .build()
      .toURL();

    expect(result).toEqual(`${urlPrefix}?rangetype=relative&from=300`);
  });

  it('includes the streams', () => {
    const result = SearchLink.builder()
      .streams(['stream1', 'otherstream', 'weird:stream:name'])
      .build()
      .toURL();

    expect(result).toEqual(`${urlPrefix}?streams=stream1%2Cotherstream%2Cweird%3Astream%3Aname`);
  });

  it('includes the id of a highlighted message2', () => {
    const result = SearchLink.builder()
      .highlightedMessage('f24c4629-e047-41b7-b3d2-1d30228ea532')
      .build()
      .toURL();

    expect(result).toEqual(`${urlPrefix}?highlightMessage=f24c4629-e047-41b7-b3d2-1d30228ea532`);
  });

  it('merges filter fields into the query string', () => {
    const result = SearchLink.builder()
      .query(createElasticsearchQueryString('foo:bar'))
      .filterFields({ foo: 42, something: 'else' })
      .build()
      .toURL();

    expect(result).toEqual(`${urlPrefix}?q=foo%3Abar+AND+foo%3A%2242%22+AND+something%3A%22else%22`);
  });

  it('combines all parameters', () => {
    const result = SearchLink.builder()
      .id('aGreatSearch')
      .query(createElasticsearchQueryString('_exists_:nf_version'))
      .timerange({ type: 'keyword', keyword: 'yesterday' })
      .filterFields({ threat: true, nf_src_address_city_name: 'Berlin' })
      .highlightedMessage('cd3b6032-4afe-42bd-9166-80ee28eac6c0')
      .streams(['oneStream', 'anotherStream'])
      .build()
      .toURL();

    expect(result).toEqual(
      `${urlPrefix}/aGreatSearch?`
      + 'rangetype=keyword&keyword=yesterday'
      + '&q=_exists_%3Anf_version+AND+threat%3A%22true%22+AND+nf_src_address_city_name%3A%22Berlin%22'
      + '&highlightMessage=cd3b6032-4afe-42bd-9166-80ee28eac6c0'
      + '&streams=oneStream%2CanotherStream',
    );
  });
});
