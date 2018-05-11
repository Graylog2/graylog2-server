import Reflux from 'reflux';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';
import ObjectUtils from 'util/ObjectUtils';

const CatalogActions = ActionsProvider.getActions('Catalog');

const CatalogStore = Reflux.createStore({
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

  getSelectedEntities(requestedEntities) {
    /* The API will not accept the title in the request payload, so I have
       to remove it. Also I need to flatten it. */
    const payload = Object.keys(requestedEntities).reduce((result, key) => {
      return result.concat(requestedEntities[key].map((entity) => {
        const newEntity = ObjectUtils.clone(entity);
        delete newEntity.title;
        return newEntity;
      }));
    }, []);
    const url = URLUtils.qualifyUrl(ApiRoutes.CatalogsController.queryEntities().url);
    const promise = fetch('POST', url, { entities: payload })
      .then((entities) => {
        return entities.entities;
      });
    CatalogActions.getSelectedEntities.promise(promise);
  },
});

export default CatalogStore;
