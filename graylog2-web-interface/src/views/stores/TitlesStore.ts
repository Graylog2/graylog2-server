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
import * as Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import type { RefluxActions } from 'stores/StoreTypes';
import { singletonActions, singletonStore } from 'views/logic/singleton';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';
import type { TitlesMap, TitleType } from './TitleTypes';

type TitlesActionsTypes = RefluxActions<{
  set: (type: string, id: string, title: string) => Promise<TitlesMap>,
}>;

export const TitlesActions: TitlesActionsTypes = singletonActions(
  'views.Titles',
  () => Reflux.createActions({
    set: { asyncResult: true },
  }),
);

export { default as TitleTypes } from './TitleTypes';

export const TitlesStore = singletonStore(
  'views.Titles',
  () => Reflux.createStore({
    listenables: [TitlesActions],

    titles: Immutable.Map<TitleType, Immutable.Map<string, string>>(),

    init() {
      this.listenTo(CurrentViewStateStore, this.onViewStateStoreChange, this.onViewStateStoreChange);
    },
    getInitialState() {
      return this.titles;
    },
    onViewStateStoreChange({ state }) {
      const titles = get(state, 'titles');

      if (!isEqual(titles, this.titles)) {
        this.titles = titles;
        this._trigger();
      }
    },

    set(type, id, title) {
      this.titles = this.titles.setIn([type, id], title);
      const promise = CurrentViewStateActions.titles(this.titles).then(() => this.titles);

      this._trigger();
      TitlesActions.set.promise(promise);

      return promise;
    },

    _trigger() {
      this.trigger(this.titles);
    },
  }),
);
