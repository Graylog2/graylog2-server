import Reflux from 'reflux';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const CatalogActions = ActionsProvider.getActions('Catalog');

const CatalogStores = Reflux.createStore({
  listenables: [CatalogActions],

  showEntityIndex() {
    const url = URLUtils.qualifyUrl(ApiRoutes.CatalogsController.showEntityIndex().url);
    const promise = fetch('GET', url)
      .then((result) => {
        const entityIndex = lodash.groupBy(result.entities, 'type');
        this.trigger({ entityIndex: entityIndex });

        return result;
      });

    CatalogActions.showEntityIndex.promise(promise);
  },
});

export default CatalogStores;
