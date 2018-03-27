import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const ContentPacksActions = ActionsProvider.getActions('ContentPacks');

const ContentPacksStores = Reflux.createStore({
  listenables: [ContentPacksActions],

  get(contentPackId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.get(contentPackId).url);
    const promise = fetch('GET', url)
      .then((result) => {
        const versions = Object.keys(result);
        this.trigger({ contentPack: result, selectedVersion: versions[0] });

        return result;
      });

    ContentPacksActions.get.promise(promise);
  },

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.list().url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ contentPacks: result });

        return result;
      });

    ContentPacksActions.list.promise(promise);
  },

  create(request) {
    const promise = fetch('POST', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.create().url), request);

    ContentPacksActions.create.promise(promise);
  },

  delete(contentPackId) {
    const promise = fetch('DELETE', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.delete(contentPackId).url));
    ContentPacksActions.delete.promise(promise);
  },

  deleteRev(contentPackId, revision) {
    const promise = fetch('DELETE', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.deleteRev(contentPackId, revision).url));
    ContentPacksActions.deleteRev.promise(promise);
  },
});

export default ContentPacksStores;
