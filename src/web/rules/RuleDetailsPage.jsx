import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';

import Rule from './Rule';
import RulesStore from './RulesStore';
import RulesActions from './RulesActions';

function filterRules(state) {
  return state.rules ? state.rules.filter(r => r.id === this.props.params.ruleId)[0] : undefined;
}

const RuleDetailsPage = React.createClass({
  propTypes: {
    params: React.PropTypes.object.isRequired,
    history: React.PropTypes.object.isRequired,
  },

  mixins: [Reflux.connectFilter(RulesStore, 'rule', filterRules)],

  componentDidMount() {
    if (this.props.params.ruleId !== 'new') {
      RulesActions.get(this.props.params.ruleId);
    }
  },

  _save(rule, callback) {
    let promise;
    if (rule.id) {
      promise = RulesActions.update.triggerPromise(rule);
    } else {
      promise = RulesActions.save.triggerPromise(rule);
    }
    promise.then(() => callback());
  },

  _validateRule(rule, setErrorsCb) {
    RulesActions.parse(rule, setErrorsCb);
  },

  _isLoading() {
    return this.props.params.ruleId !== 'new' && !this.state.rule;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <Rule rule={this.state.rule} create={this.props.params.ruleId === 'new'} onSave={this._save}
            validateRule={this._validateRule} history={this.props.history} />
    );
  },
});

export default RuleDetailsPage;