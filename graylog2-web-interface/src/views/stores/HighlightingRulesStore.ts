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
// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import type { RefluxActions, Store } from 'stores/StoreTypes';
import ViewState from 'views/logic/views/ViewState';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { Value } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { singletonActions, singletonStore } from 'views/logic/singleton';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

type HighlightingRulesActionsType = RefluxActions<{
  add: (rule: HighlightingRule) => Promise<Array<HighlightingRule>>,
  remove: (rule: HighlightingRule) => Promise<Array<HighlightingRule>>,
  update: (rule: HighlightingRule) => Promise<Array<HighlightingRule>>,
}>;

const HighlightingRulesActions: HighlightingRulesActionsType = singletonActions(
  'views.HighlightingRules',
  () => Reflux.createActions({
    add: { asyncResult: true },
    remove: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

class Key extends Immutable.Record({ field: null, value: undefined }) {}

const makeKey = (field: string, value: Value) => new Key({ field, value });

type StateType = Immutable.OrderedMap<Key, string>;

const HighlightingRulesStore: Store<Array<HighlightingRule>> = singletonStore(
  'views.HighlightingRules',
  () => Reflux.createStore({
    listenables: [HighlightingRulesActions],

    state: Immutable.OrderedMap<Key, string>(),

    init() {
      this.listenTo(CurrentViewStateStore, this.onViewStateStoreChange, this.onViewStateStoreChange);
    },

    onViewStateStoreChange({ state }: { state: ViewState }) {
      const formatting: FormattingSettings = get(state, 'formatting', FormattingSettings.empty());

      this.formatting = formatting;
      const { highlighting } = formatting;
      const rules = highlighting.reduce(
        (prev: StateType, rule: HighlightingRule) => prev.set(makeKey(rule.field, rule.value), rule.color),
        Immutable.OrderedMap<Key, string>(),
      );

      if (!isEqual(rules, this.state)) {
        this.state = rules;
        this._trigger();
      }
    },

    getInitialState(): Array<HighlightingRule> {
      return this._state();
    },
    _state(): Array<HighlightingRule> {
      return this.state.reduce((prev, color, key) => [
        ...prev,
        HighlightingRule.builder()
          .field(key.field)
          .value(key.value)
          .color(color)
          .build(),
      ], []);
    },
    _trigger() {
      this.trigger(this._state());
    },

    add(rule: HighlightingRule): Promise<Array<HighlightingRule>> {
      const { field, value, color } = rule;
      const key = makeKey(field, value);
      const promise = (this.state.has(key) ? Promise.resolve() : this._propagateAndTrigger(this.state.set(key, color)))
        .then(() => this._state());

      HighlightingRulesActions.add.promise(promise);

      return promise;
    },
    remove(rule: HighlightingRule): Promise<Array<HighlightingRule>> {
      const { field, value } = rule;
      const key = makeKey(field, value);
      const promise = (this.state.has(key) ? this._propagateAndTrigger(this.state.delete(key)) : Promise.resolve())
        .then(() => this._state());

      HighlightingRulesActions.remove.promise(promise);

      return promise;
    },
    update(rule: HighlightingRule): Promise<Array<HighlightingRule>> {
      const { field, value, color } = rule;
      const key = makeKey(field, value);
      const promise = this._propagateAndTrigger(this.state.set(key, color));

      HighlightingRulesActions.update.promise(promise);

      return promise;
    },
    _propagateAndTrigger(newState: Immutable.OrderedMap<Immutable.Map<string, any>, string>) {
      const newHighlighting = newState.entrySeq().map(([key, color]) => {
        const { field, value } = key.toJS();

        return HighlightingRule.create(field, value, undefined, color);
      }).toJS();
      const newFormatting = this.formatting.toBuilder().highlighting(newHighlighting).build();

      return CurrentViewStateActions.formatting(newFormatting);
    },
  }),
);

export {
  HighlightingRulesActions,
  HighlightingRulesStore,
};
