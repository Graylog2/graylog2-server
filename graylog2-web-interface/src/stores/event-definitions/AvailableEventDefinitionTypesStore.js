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
