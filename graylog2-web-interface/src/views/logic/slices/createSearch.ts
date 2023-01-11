import { qualifyUrl } from 'util/URLUtils';
import Search from 'views/logic/search/Search';
import fetch from 'logic/rest/FetchProvider';

const createSearchUrl = qualifyUrl('/views/search');
const createSearch = (search: Search) => fetch('POST', createSearchUrl, JSON.stringify(search))
  .then((response) => Search.fromJSON(response));

export default createSearch;
