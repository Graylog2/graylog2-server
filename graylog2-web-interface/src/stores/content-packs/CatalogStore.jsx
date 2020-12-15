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
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';
import EntityIndex from 'logic/content-packs/EntityIndex';

const CatalogActions = ActionsProvider.getActions('Catalog');

const CatalogStore = Reflux.createStore({
  listenables: [CatalogActions],

  showEntityIndex() {
    const url = URLUtils.qualifyUrl(ApiRoutes.CatalogsController.showEntityIndex().url);
    const promise = fetch('GET', url)
      .then((result) => {
        const entityIndex = lodash.groupBy(result.entities.map((e) => EntityIndex.fromJSON(e)), 'type.name');

        this.trigger({ entityIndex: entityIndex });

        return result;
      });

    CatalogActions.showEntityIndex.promise(promise);
  },

  getSelectedEntities(requestedEntities) {
    const payload = Object.keys(requestedEntities).reduce((result, key) => {
      return result.concat(requestedEntities[key]
        .filter((entitiy) => entitiy instanceof EntityIndex)
        .map((entity) => entity.toJSON()));
    }, []);
    const url = URLUtils.qualifyUrl(ApiRoutes.CatalogsController.queryEntities().url);
    const promise = fetch('POST', url, { entities: payload });

    CatalogActions.getSelectedEntities.promise(promise);
  },
});

export default CatalogStore;
