import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { AvailableEventDefinitionTypesActions } = CombinedProvider.get('AvailableEventDefinitionTypes');

const AvailableEventDefinitionTypesStore = Reflux.createStore({
  listenables: [AvailableEventDefinitionTypesActions],
  sourceUrl: '/events/entity_types',
  entityTypes: undefined,

  init() {
    this.get();
  },

  getInitialState() {
    return this.entityTypes;
  },

  get() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));

    promise.then((response) => {
      this.entityTypes = response;
      this.trigger(this.entityTypes);
    });
  },
});

export default AvailableEventDefinitionTypesStore;
