import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { DocumentTitle, Spinner } from 'components/common';

import Rule from 'components/rules/Rule';
import CombinedProvider from 'injection/CombinedProvider';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');
const { PipelinesStore, PipelinesActions } = CombinedProvider.get('Pipelines');

function filterRules(state) {
  return state.rules ? state.rules.filter((r) => r.id === this.props.params.ruleId)[0] : undefined;
}

const RuleDetailsPage = createReactClass({
  displayName: 'RuleDetailsPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connectFilter(RulesStore, 'rule', filterRules), Reflux.connect(PipelinesStore)],

  componentDidMount() {
    if (this.props.params.ruleId !== 'new') {
      PipelinesActions.list();
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
    return this.props.params.ruleId !== 'new' && !(this.state.rule && this.state.pipelines);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const pipelinesUsingRule = this.props.params.ruleId === 'new' ? [] : this.state.pipelines.filter((pipeline) => {
      return pipeline.stages.some((stage) => stage.rules.indexOf(this.state.rule.title) !== -1);
    });

    const pageTitle = (this.props.params.ruleId === 'new' ? 'New pipeline rule' : `Pipeline rule ${this.state.rule.title}`);

    return (
      <DocumentTitle title={pageTitle}>
        <Rule rule={this.state.rule}
              usedInPipelines={pipelinesUsingRule}
              create={this.props.params.ruleId === 'new'}
              onSave={this._save}
              validateRule={this._validateRule} />
      </DocumentTitle>
    );
  },
});

export default RuleDetailsPage;
