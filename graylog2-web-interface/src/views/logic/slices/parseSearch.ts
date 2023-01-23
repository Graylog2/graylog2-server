import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type Search from 'views/logic/search/Search';
import SearchMetadata from 'views/logic/search/SearchMetadata';

const parseSearchUrl = URLUtils.qualifyUrl('/views/search/metadata');

const parseSearch = (search: Search): Promise<SearchMetadata> => fetch('POST', parseSearchUrl, JSON.stringify(search))
  .then((response) => SearchMetadata.fromJSON(response), () => undefined);

export default parseSearch;
