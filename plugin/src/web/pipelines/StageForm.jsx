import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { SelectableList } from 'components/common';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import RulesStore from 'rules/RulesStore';

import Routes from 'routing/Routes';

const StageForm = React.createClass({
  propTypes: {
    stage: PropTypes.object,
    create: React.PropTypes.bool,
    save: React.PropTypes.func.isRequired,
    validateStage: React.PropTypes.func.isRequired,
  },
  mixins: [Reflux.connect(RulesStore)],

  getDefaultProps() {
    return {
      stage: {
        stage: 0,
        match_all: false,
        rules: [],
      },
    };
  },

  getInitialState() {
    const stage = ObjectUtils.clone(this.props.stage);
    return {
      // when editing, take the stage that's been passed in
      stage: {
        stage: stage.stage,
        match_all: stage.match_all,
        rules: stage.rules,
      },
    };
  },

  openModal() {
    this.refs.modal.open();
  },

  _onChange(event) {
    const stage = ObjectUtils.clone(this.state.stage);
    stage[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState({ stage });
  },

  _onRulesChange(newRules) {
    const stage = ObjectUtils.clone(this.state.stage);
    stage.rules = newRules;
    this.setState({ stage });
  },

  _closeModal() {
    this.refs.modal.close();
  },

  _saved() {
    this._closeModal();
    if (this.props.create) {
      this.setState(this.getInitialState());
    }
  },

  _save() {
    this.props.save(this.state.stage, this._saved);
  },

  _getFormattedOptions(rules) {
    return rules ? rules.map(rule => {
      return { value: rule.title, label: rule.title };
    }) : [];
  },

  render() {
    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Add new stage';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }

    const rulesHelp = (
      <span>
        Select the rules evaluated on this stage, or create one in the{' '}
        <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_RULES')}><a>Pipeline Rules page</a></LinkContainer>.
      </span>
    );

    return (
      <span>
        <Button onClick={this.openModal}
                bsStyle={this.props.create ? 'success' : 'info'}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalForm ref="modal"
                            title={`${this.props.create ? 'Add new' : 'Edit'} stage ${this.state.stage.stage}`}
                            onSubmitForm={this._save}
                            submitButtonText="Save">
          <fieldset>
            <Input type="number"
                   id="stage"
                   name="stage"
                   label="Stage"
                   autoFocus
                   onChange={this._onChange}
                   help="Stage priority. The lower the number, the earlier it will execute."
                   value={this.state.stage.stage} />

            <Input label="Continue processing on next stage when">
              <Input type="radio"
                     id="match_all"
                     name="match_all"
                     value="true"
                     label="All rules on this stage match the message"
                     onChange={this._onChange}
                     checked={this.state.stage.match_all} />

              <Input type="radio"
                     id="match_any"
                     name="match_all"
                     value="false"
                     label="At least one of the rules on this stage matches the message"
                     onChange={this._onChange}
                     checked={!this.state.stage.match_all} />

              <Input label="Stage rules"
                     help={rulesHelp}>
                <SelectableList options={this._getFormattedOptions(this.state.rules)} isLoading={!this.state.rules}
                                onChange={this._onRulesChange} selectedOptions={this.state.stage.rules} />
              </Input>
            </Input>
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default StageForm;
