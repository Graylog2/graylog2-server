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
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { cloneDeep } from 'lodash';

import { Link } from 'components/graylog/router';
import { Button, FormGroup, ControlLabel } from 'components/graylog';
import { SelectableList } from 'components/common';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { RulesStore } = CombinedProvider.get('Rules');

const StageForm = createReactClass({
  displayName: 'StageForm',

  propTypes: {
    pipeline: PropTypes.object.isRequired,
    stage: PropTypes.object,
    create: PropTypes.bool,
    save: PropTypes.func.isRequired,
  },

  mixins: [Reflux.connect(RulesStore)],

  getDefaultProps() {
    return {
      create: false,
      stage: {
        stage: undefined,
        match_all: false,
        rules: [],
      },
    };
  },

  getInitialState() {
    const { create, pipeline, stage } = this.props;
    const initialStageNumber = (create ? Math.max(...pipeline.stages.map((s) => s.stage)) + 1 : stage.stage);

    return {
      initialStageNumber: initialStageNumber,
      // when editing, take the stage that's been passed in
      stage: { ...cloneDeep(stage), stage: initialStageNumber },
    };
  },

  openModal() {
    this.modal.open();
  },

  _onChange(event) {
    let { stage } = this.state;

    stage = cloneDeep(stage);
    stage[event.target.name] = getValueFromInput(event.target);
    this.setState({ stage });
  },

  _onRulesChange(newRules) {
    let { stage } = this.state;

    stage = cloneDeep(stage);
    stage.rules = newRules;
    this.setState({ stage });
  },

  _closeModal() {
    this.modal.close();
  },

  _isOverridingStage() {
    const { pipeline } = this.props;
    const { initialStageNumber, stage } = this.state;

    return (stage.stage !== initialStageNumber && pipeline.stages.some(({ stage: s }) => s === stage.stage));
  },

  _saved() {
    this._closeModal();
  },

  _save() {
    const { stage } = this.state;
    const { save } = this.props;

    if (!this._isOverridingStage()) {
      save(stage, this._saved);
    }
  },

  _getFormattedOptions(rules) {
    const { stage } = this.state;
    const chosenRules = stage.rules;

    return rules ? rules.filter((rule) => this._filterChosenRules(rule, chosenRules)).map(this._formatRuleOption) : [];
  },

  _formatRuleOption(rule) {
    return { value: rule.title, label: rule.title };
  },

  _filterChosenRules(rule, chosenRules) {
    return !chosenRules.includes(rule.title);
  },

  render() {
    let triggerButtonContent;
    const { create } = this.props;
    const { stage, rules } = this.state;

    if (create) {
      triggerButtonContent = 'Add new stage';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }

    const rulesHelp = (
      <span>
        Select the rules evaluated on this stage, or create one in the{' '}
        <Link to={Routes.SYSTEM.PIPELINES.RULES}>Pipeline Rules page</Link>.
      </span>
    );

    const isOverridingStage = this._isOverridingStage();

    return (
      <span>
        <Button onClick={this.openModal}
                bsStyle={create ? 'success' : 'info'}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                            title={`${create ? 'Add new' : 'Edit'} stage ${stage.stage}`}
                            onSubmitForm={this._save}
                            submitButtonText="Save">
          <fieldset>
            <Input type="number"
                   id="stage"
                   name="stage"
                   label="Stage"
                   autoFocus
                   onChange={this._onChange}
                   bsStyle={isOverridingStage ? 'error' : null}
                   help={isOverridingStage
                     ? 'Stage is already in use, please use another number or edit the existing stage.'
                     : 'Stage priority. The lower the number, the earlier it will execute.'}
                   value={stage.stage} />

            <FormGroup>
              <ControlLabel>Continue processing on next stage when</ControlLabel>
            </FormGroup>

            <Input type="radio"
                   id="match_all"
                   name="match_all"
                   value="true"
                   label="All rules on this stage match the message"
                   onChange={this._onChange}
                   checked={stage.match_all} />

            <Input type="radio"
                   id="match_any"
                   name="match_all"
                   value="false"
                   label="At least one of the rules on this stage matches the message"
                   onChange={this._onChange}
                   checked={!stage.match_all} />

            <Input id="stage-rules-select"
                   label="Stage rules"
                   help={rulesHelp}>
              <SelectableList options={this._getFormattedOptions(rules)}
                              isLoading={!rules}
                              onChange={this._onRulesChange}
                              selectedOptions={stage.rules} />
            </Input>
          </fieldset>
        </BootstrapModalForm>
      </span>
    );
  },
});

export default StageForm;
