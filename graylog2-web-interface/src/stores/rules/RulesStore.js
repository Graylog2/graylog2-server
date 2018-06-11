import Reflux from 'reflux';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import naturalSort from 'javascript-natural-sort';
import CombinedProvider from 'injection/CombinedProvider';

const { RulesActions } = CombinedProvider.get('Rules');

const RulesStore = Reflux.createStore({
  listenables: [RulesActions],
  rules: undefined,
  functionDescriptors: undefined,

  getInitialState() {
    return { rules: this.rules, functionDescriptors: this.functionDescriptors };
  },

  _updateRulesState(rule) {
    if (!this.rules) {
      this.rules = [rule];
    } else {
      const doesRuleExist = this.rules.some(r => r.id === rule.id);
      if (doesRuleExist) {
        this.rules = this.rules.map(r => (r.id === rule.id ? rule : r));
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

    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.list().url);
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

    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.get(ruleId).url);
    const promise = fetch('GET', url);
    promise.then(this._updateRulesState, failCallback);

    return promise;
  },

  save(ruleSource) {
    const failCallback = (error) => {
      UserNotification.error(`Saving rule "${ruleSource.title}" failed with status: ${error.message}`,
        `Could not save processing rule "${ruleSource.title}"`);
    };
    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.create().url);
    const rule = {
      title: ruleSource.title,
      description: ruleSource.description,
      source: ruleSource.source,
    };
    const promise = fetch('POST', url, rule);
    promise.then((response) => {
      this._updateRulesState(response);
      UserNotification.success(`Rule "${response.title}" created successfully`);
    }, failCallback);

    RulesActions.save.promise(promise);
    return promise;
  },

  update(ruleSource) {
    const failCallback = (error) => {
      UserNotification.error(`Updating rule "${ruleSource.title}" failed with status: ${error.message}`,
        `Could not update processing rule "${ruleSource.title}"`);
    };
    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.update(ruleSource.id).url);
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
    }, failCallback);

    RulesActions.update.promise(promise);
    return promise;
  },
  delete(rule) {
    const failCallback = (error) => {
      UserNotification.error(`Deleting rule "${rule.title}" failed with status: ${error.message}`,
        `Could not delete processing rule "${rule.title}"`);
    };
    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.delete(rule.id).url);
    return fetch('DELETE', url).then(() => {
      this.rules = this.rules.filter(el => el.id !== rule.id);
      this.trigger({ rules: this.rules, functionDescriptors: this.functionDescriptors });
      UserNotification.success(`Rule "${rule.title}" was deleted successfully`);
    }, failCallback);
  },
  parse(ruleSource, callback) {
    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.parse().url);
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
    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.multiple().url);
    const promise = fetch('POST', url, { rules: ruleNames });
    promise.then(callback);

    return promise;
  },
  loadFunctions() {
    if (this.functionDescriptors) {
      return undefined;
    }
    const url = URLUtils.qualifyUrl(ApiRoutes.RulesController.functions().url);
    return fetch('GET', url)
      .then(this._updateFunctionDescriptors);
  },
});

export default RulesStore;
