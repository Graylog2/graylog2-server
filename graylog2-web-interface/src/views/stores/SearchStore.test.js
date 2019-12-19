// @flow strict
import fetch from 'logic/rest/FetchProvider';

import Search from 'views/logic/search/Search';
import { SearchActions } from './SearchStore';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('SearchStore', () => {
  it('assigns a new search id when creating a search', () => {
    fetch.mockImplementation((method, url, body) => Promise.resolve(body && JSON.parse(body)));
    const newSearch = Search.create();
    return SearchActions.create(newSearch).then(({ search }) => {
      expect(search.id).not.toEqual(newSearch.id);
    });
  });
});
