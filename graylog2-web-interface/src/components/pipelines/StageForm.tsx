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
import React, { useCallback, useRef, useState } from 'react';
import { cloneDeep } from 'lodash';

import { useStore } from 'stores/connect';
import { Link } from 'components/graylog/router';
import { Button, FormGroup, ControlLabel } from 'components/graylog';
import { SelectableList } from 'components/common';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { StageType } from 'stores/pipelines/PipelinesStore';

const { RulesStore } = CombinedProvider.get('Rules');

type Props = {
  stage?: StageType,
  create: boolean,
  save: (nextStage: StageType, callback: () => void) => void,
};

const StageForm = ({ stage, create, save }: Props) => {
  const modalRef = useRef<BootstrapModalForm>();
  const [nextStage, setNextStage] = useState<StageType>(cloneDeep(stage));
  const { rules } = useStore(RulesStore);

  const openModal = () => {
    if (modalRef.current) {
      modalRef.current.open();
    }
  };

  const _onChange = ({ target }) => {
    setNextStage((currentStage) => ({ ...currentStage, [target.name]: getValueFromInput(target) }));
  };

  const _onRulesChange = (newRules) => {
    setNextStage((currentStage) => ({ ...currentStage, rules: newRules }));
  };

  const _closeModal = () => {
    if (modalRef.current) {
      modalRef.current.close();
    }
  };

  const _onSaved = () => {
    _closeModal();

    if (create) {
      // Reset form after creation
      setNextStage(cloneDeep(stage));
    }
  };

  const _handleSave = () => {
    save(nextStage, _onSaved);
  };

  const _formatRuleOption = ({ title }) => {
    return { value: title, label: title };
  };

  const _filterChosenRules = (rule, chosenRules) => {
    return !chosenRules.includes(rule.title);
  };

  const _getFormattedOptions = useCallback(() => {
    const chosenRules = nextStage.rules;

    return rules ? rules.filter((rule) => _filterChosenRules(rule, chosenRules)).map(_formatRuleOption) : [];
  }, [nextStage.rules, rules]);

  const rulesHelp = (
    <span>
      Select the rules evaluated on this stage, or create one in the{' '}
      <Link to={Routes.SYSTEM.PIPELINES.RULES}>Pipeline Rules page</Link>.
    </span>
  );

  return (
    <span>
      <Button onClick={openModal}
              bsStyle={create ? 'success' : 'info'}>
        {create ? 'Add new stage' : 'Edit'}
      </Button>
      <BootstrapModalForm ref={modalRef}
                          title={`${create ? 'Add new' : 'Edit'} stage ${nextStage.stage}`}
                          onSubmitForm={_handleSave}
                          submitButtonText="Save">
        <fieldset>
          <Input type="number"
                 id="stage"
                 name="stage"
                 label="Stage"
                 autoFocus
                 onChange={_onChange}
                 help="Stage priority. The lower the number, the earlier it will execute."
                 value={nextStage.stage} />

          <FormGroup>
            <ControlLabel>Continue processing on next stage when</ControlLabel>
          </FormGroup>

          <Input type="radio"
                 id="match_all"
                 name="match_all"
                 value="true"
                 label="All rules on this stage match the message"
                 onChange={_onChange}
                 checked={nextStage.match_all} />

          <Input type="radio"
                 id="match_any"
                 name="match_all"
                 value="false"
                 label="At least one of the rules on this stage matches the message"
                 onChange={_onChange}
                 checked={!nextStage.match_all} />

          <Input id="stage-rules-select"
                 label="Stage rules"
                 help={rulesHelp}>
            <SelectableList options={_getFormattedOptions()}
                            isLoading={!rules}
                            onChange={_onRulesChange}
                            selectedOptions={nextStage.rules} />
          </Input>
        </fieldset>
      </BootstrapModalForm>
    </span>
  );
};

StageForm.propTypes = {
  stage: PropTypes.object,
  create: PropTypes.bool,
  save: PropTypes.func.isRequired,
};

StageForm.defaultProps = {
  create: false,
  stage: {
    stage: 0,
    match_all: false,
    rules: [],
  },
};

export default StageForm;
