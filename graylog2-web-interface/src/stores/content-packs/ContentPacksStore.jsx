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
import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';
import ContentPackRevisions from 'logic/content-packs/ContentPackRevisions';

const ContentPacksActions = ActionsProvider.getActions('ContentPacks');

const ContentPacksStore = Reflux.createStore({
  listenables: [ContentPacksActions],

  get(contentPackId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.get(contentPackId).url);
    const promise = fetch('GET', url)
      .then((response) => {
        const contentPackRevision = new ContentPackRevisions(response.content_pack_revisions);
        const constraints = response.constraints_result;
        const result = {
          contentPackRevisions: contentPackRevision,
          selectedVersion: contentPackRevision.latestRevision,
          constraints: constraints,
        };

        this.trigger(result);

        return result;
      });

    ContentPacksActions.get.promise(promise);
  },

  getRev(contentPackId, contentPackRev) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.getRev(contentPackId, contentPackRev).url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ contentPack: result.content_pack });

        return result;
      });

    ContentPacksActions.getRev.promise(promise);
  },

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.list().url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ contentPacks: result.content_packs, contentPackMetadata: result.content_packs_metadata });

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

    ContentPacksActions.installList.promise(promise);
  },
  uninstall(contentPackId, installId) {
    const promise = fetch('DELETE', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.uninstall(contentPackId, installId).url));

    ContentPacksActions.uninstall.promise(promise);
  },
  uninstallDetails(contentPackId, installId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.uninstallDetails(contentPackId, installId).url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ uninstallEntities: result.entities });

        return result;
      });

    ContentPacksActions.uninstallDetails.promise(promise);
  },
});

export default ContentPacksStore;
