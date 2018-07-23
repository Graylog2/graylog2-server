import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const ContentPacksActions = ActionsProvider.getActions('ContentPacks');

const ContentPacksStore = Reflux.createStore({
  listenables: [ContentPacksActions],

  get(contentPackId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.get(contentPackId).url);
    const promise = fetch('GET', url)
      .then((result) => {
        const contentPack = result.content_pack_revisions;
        const versions = Object.keys(contentPack);
        this.trigger({ contentPack: contentPack, selectedVersion: versions[0] });

        return result;
      });

    ContentPacksActions.get.promise(promise);
  },

  getRev(contentPackId, contentPackRev) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.getRev(contentPackId, contentPackRev).url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ contentPack: result });
        return result;
      });

    ContentPacksActions.getRev.promise(promise);
  },

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.list().url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ contentPacks: result.content_packs });

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

  install(contentPackId, revision, parameters) {
    const promise = fetch('POST', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.install(contentPackId, revision).url), parameters);

    ContentPacksActions.install.promise(promise);
  },
  installList(contentPackId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.installList(contentPackId).url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ installations: result.installations });

        return result;
      });

    ContentPacksActions.listInstall.promise(promise);
  },
  uninstall(contentPackId, installId) {
    const promise = fetch('DELETE', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.uninstall(contentPackId, installId).url));
    ContentPacksActions.uninstall.promise(promise);
  },
});

export default ContentPacksStore;
