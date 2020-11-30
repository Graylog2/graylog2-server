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
import naturalSort from 'javascript-natural-sort';

import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { RulesActions } = CombinedProvider.get('Rules');

const RulesStore = Reflux.createStore({
  listenables: [RulesActions],
  rules: undefined,
  functionDescriptors: undefined,
  metricsConfig: undefined,

  getInitialState() {
    return { rules: this.rules, functionDescriptors: this.functionDescriptors, metricsConfig: this.metricsConfig };
  },

  _updateRulesState(rule) {
    if (!this.rules) {
      this.rules = [rule];
    } else {
      const doesRuleExist = this.rules.some((r) => r.id === rule.id);

      if (doesRuleExist) {
        this.rules = this.rules.map((r) => (r.id === rule.id ? rule : r));
      } else {
        this.rules.push(rule);
      }
    }

    this.trigger({ rules: this.rules, functionDescriptors: this.functionDescriptors });
  },

  _updateFunctionDescriptors(functions) {
    if (functions) {
      this.functionDescriptors = functions.sort((fn1, fn2) => naturalSort(fn1.name, fn2.name));
    }

    this.trigger({ rules: this.rules, functionDescriptors: this.functionDescriptors });
  },

  list() {
    const failCallback = (error) => {
      UserNotification.error(`Fetching rules failed with status: ${error.message}`,
        'Could not retrieve processing rules');
    };

    const url = qualifyUrl(ApiRoutes.RulesController.list().url);

    return fetch('GET', url).then((response) => {
      this.rules = response;
      this.trigger({ rules: response, functionDescriptors: this.functionDescriptors });
    }, failCallback);
  },

  get(ruleId) {
    const failCallback = (error) => {
      UserNotification.error(`Fetching rule "${ruleId}" failed with status: ${error.message}`,
        `Could not retrieve processing rule "${ruleId}"`);
    };

    const url = qualifyUrl(ApiRoutes.RulesController.get(ruleId).url);
    const promise = fetch('GET', url);

    promise.then(this._updateRulesState, failCallback);

    return promise;
  },

  save(ruleSource) {
    const failCallback = (error) => {
      UserNotification.error(`Saving rule "${ruleSource.title}" failed with status: ${error.message}`,
        `Could not save processing rule "${ruleSource.title}"`);
    };

    const url = qualifyUrl(ApiRoutes.RulesController.create().url);
    const rule = {
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source,
    };
    const promise = fetch('POST', url, rule);

    promise.then((response) => {
      this._updateRulesState(response);
      UserNotification.success(`Rule "${response.title}" created successfully`);

      return response;
    }, failCallback);

    RulesActions.save.promise(promise);

    return promise;
  },

  update(ruleSource) {
    const failCallback = (error) => {
      UserNotification.error(`Updating rule "${ruleSource.title}" failed with status: ${error.message}`,
        `Could not update processing rule "${ruleSource.title}"`);
    };

    const url = qualifyUrl(ApiRoutes.RulesController.update(ruleSource.id).url);
    const rule = {
      id: ruleSource.id,
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source,
    };
    const promise = fetch('PUT', url, rule);

    promise.then((response) => {
      this._updateRulesState(response);
      UserNotification.success(`Rule "${response.title}" updated successfully`);

      return response;
    }, failCallback);

    RulesActions.update.promise(promise);

    return promise;
  },
  delete(rule) {
    const failCallback = (error) => {
      UserNotification.error(`Deleting rule "${rule.title}" failed with status: ${error.message}`,
        `Could not delete processing rule "${rule.title}"`);
    };

    const url = qualifyUrl(ApiRoutes.RulesController.delete(rule.id).url);

    return fetch('DELETE', url).then(() => {
      this.rules = this.rules.filter((el) => el.id !== rule.id);
      this.trigger({ rules: this.rules, functionDescriptors: this.functionDescriptors });
      UserNotification.success(`Rule "${rule.title}" was deleted successfully`);
    }, failCallback);
  },
  parse(ruleSource, callback) {
    const url = qualifyUrl(ApiRoutes.RulesController.parse().url);
    const rule = {
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source,
    };

    return fetch('POST', url, rule).then(
      (response) => {
        // call to clear the errors, the parsing was successful
        callback([]);

        return response;
      },
      (error) => {
        // a Bad Request indicates a parse error, set all the returned errors in the editor
        const response = error.additional.res;

        if (response.status === 400) {
          callback(response.body);
        }
      },
    );
  },
  multiple(ruleNames, callback) {
    const url = qualifyUrl(ApiRoutes.RulesController.multiple().url);
    const promise = fetch('POST', url, { rules: ruleNames });

    promise.then(callback);

    return promise;
  },
  loadFunctions() {
    if (this.functionDescriptors) {
      return undefined;
    }

    const url = qualifyUrl(ApiRoutes.RulesController.functions().url);

    return fetch('GET', url)
      .then(this._updateFunctionDescriptors);
  },
  loadMetricsConfig() {
    const url = qualifyUrl(ApiRoutes.RulesController.metricsConfig().url);
    const promise = fetch('GET', url);

    promise.then(
      (response) => {
        this.metricsConfig = response;
        this.trigger({ rules: this.rules, functionDescriptors: this.functionDescriptors, metricsConfig: this.metricsConfig });
      },
      (error) => {
        UserNotification.error(`Couldn't load rule metrics config: ${error.message}`, "Couldn't load rule metrics config");
      },
    );

    RulesActions.loadMetricsConfig.promise(promise);
  },
  updateMetricsConfig(nextConfig) {
    const url = qualifyUrl(ApiRoutes.RulesController.metricsConfig().url);
    const promise = fetch('PUT', url, nextConfig);

    promise.then(
      (response) => {
        this.metricsConfig = response;
        this.trigger({ rules: this.rules, functionDescriptors: this.functionDescriptors, metricsConfig: this.metricsConfig });
        UserNotification.success('Successfully updated rule metrics config');
      },
      (error) => {
        UserNotification.error(`Couldn't update rule metrics config: ${error.message}`, "Couldn't update rule metrics config");
      },
    );

    RulesActions.updateMetricsConfig.promise(promise);
  },
});

export default RulesStore;
