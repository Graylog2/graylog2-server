// @flow strict
import Reflux from 'reflux';

// $FlowFixMe: imports from core need to be fixed in flow
import fetch from 'logic/rest/FetchProvider';
// $FlowFixMe: imports from core need to be fixed in flow
import URLUtils from 'util/URLUtils';
import SearchMetadata from 'enterprise/logic/search/SearchMetadata';

const parseSearchUrl = URLUtils.qualifyUrl('/views/search/metadata');
const parseSearchIdUrl = id => URLUtils.qualifyUrl(`/views/search/metadata/${id}`);

export type SearchMetadataActionsType = {
  parseSearch: (any) => Promise<SearchMetadata>,
  parseSearchId: (string) => Promise<SearchMetadata>,
};

export const SearchMetadataActions: SearchMetadataActionsType = Reflux.createActions({
  parseSearch: { asyncResult: true },
  parseSearchId: { asyncResult: true },
});

export const SearchMetadataStore = Reflux.createStore({
  listenables: [SearchMetadataActions],

  state: SearchMetadata.empty(),

  getInitialState() {
    return this.state;
  },

  parseSearch(searchRequest): Promise<SearchMetadata> {
    const promise = fetch('POST', parseSearchUrl, JSON.stringify(searchRequest))
      .then(SearchMetadata.fromJSON)
      .then((metadata) => {
        this.state = metadata;
        this._trigger();
        return this.state;
      });
    SearchMetadataActions.parseSearch.promise(promise);
    return promise;
  },

  parseSearchId(searchId: string): Promise<SearchMetadata> {
    const promise = fetch('GET', parseSearchIdUrl, searchId)
      .then(SearchMetadata.fromJSON)
      .then((metadata) => {
        this.state = metadata;
        this._trigger();
        return this.state;
      });
    SearchMetadataActions.parseSearchId.promise(promise);
    return promise;
  },

  _trigger() {
    this.trigger(this.state);
  },
});
