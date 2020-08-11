import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
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
    const { params } = this.props;

    if (params.ruleId !== 'new') {
      PipelinesActions.list();
      RulesActions.get(params.ruleId);
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
    const { params } = this.props;
    const { rule, pipelines } = this.state;

    return params.ruleId !== 'new' && !(rule && pipelines);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { params } = this.props;
    const { pipelines, rule } = this.state;

    const pipelinesUsingRule = params.ruleId === 'new' ? [] : pipelines.filter((pipeline) => {
      return pipeline.stages.some((stage) => stage.rules.indexOf(rule.title) !== -1);
    });

    const pageTitle = (params.ruleId === 'new' ? 'New pipeline rule' : `Pipeline rule ${rule.title}`);

    return (
      <DocumentTitle title={pageTitle}>
        <Rule rule={rule}
              usedInPipelines={pipelinesUsingRule}
              create={params.ruleId === 'new'}
              onSave={this._save}
              validateRule={this._validateRule} />
      </DocumentTitle>
    );
  },
});

export default RuleDetailsPage;
