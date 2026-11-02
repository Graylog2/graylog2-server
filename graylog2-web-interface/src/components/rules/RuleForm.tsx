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
import React, { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button, Col, ControlLabel, FormControl, FormGroup, Row, Input } from 'components/bootstrap';
import { ConfirmLeaveDialog, SourceCodeEditor, FormSubmit } from 'components/common';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

import { PipelineRulesContext } from './RuleContext';
import PipelinesUsingRule from './PipelinesUsingRule';
import RuleSimulation from './RuleSimulation';

type Props = {
  create?: boolean;
  isManaged?: boolean;
};

const RuleForm = ({ create = false, isManaged = false }: Props) => {
  const { t } = useTranslation('pipelines');
  const {
    description,
    handleDescription,
    handleSavePipelineRule,
    ruleSourceRef,
    onAceLoaded,
    onChangeSource,
    ruleSource,
  } = useContext(PipelineRulesContext);

  const [isDirty, setIsDirty] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const history = useHistory();

  const handleError = (error: { responseMessage: string }) => {
    if (error?.responseMessage?.includes('duplicate key error')) {
      const duplicatedTitle = error.responseMessage.match(/title: "(.*)"/i)[1];
      setErrorMessage(t('rule_form.duplicate_title', { title: duplicatedTitle }));
    }
  };

  const handleCancel = () => {
    setErrorMessage('');
    setIsDirty(false);
    history.push(Routes.SYSTEM.PIPELINES.RULES);
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    setIsDirty(false);
    event.preventDefault();

    handleSavePipelineRule(handleCancel, handleError);
  };

  const handleApply = () => {
    handleSavePipelineRule((rule) => {
      setErrorMessage('');
      setIsDirty(false);
      history.push(Routes.SYSTEM.PIPELINES.RULE(rule.id));
    }, handleError);
  };

  const handleDescriptionChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setIsDirty(true);
    handleDescription(event.target.value);
  };

  const handleSourceChange = (newSource: string) => {
    setErrorMessage('');
    setIsDirty(true);
    onChangeSource(newSource);
  };

  return (
    <form onSubmit={handleSubmit}>
      <fieldset>
        <FormGroup id="ruleTitleInformation">
          <ControlLabel>{t('rule_form.title')}</ControlLabel>
          <FormControl.Static>{t('rule_form.title_help')}</FormControl.Static>
        </FormGroup>

        {isDirty && <ConfirmLeaveDialog question={t('rule_form.leave_question')} />}

        <Input
          type="textarea"
          id="description"
          label={t('rule_form.description')}
          value={description}
          onChange={handleDescriptionChange}
          autoFocus
          rows={1}
          help={t('rule_form.description_help')}
        />

        <PipelinesUsingRule create={create} />

        <Input
          id="rule-source-editor"
          label={t('rule_form.source')}
          help={t('rule_form.source_help')}
          error={errorMessage}>
          <SourceCodeEditor
            id={`source${create ? '-create' : '-edit'}`}
            mode="pipeline"
            onLoad={onAceLoaded}
            onChange={handleSourceChange}
            value={ruleSource}
            innerRef={ruleSourceRef}
          />
        </Input>

        <RuleSimulation />
      </fieldset>

      <Row>
        <Col md={12}>
          <FormSubmit
            submitButtonText={create ? t('rule_form.create_submit') : t('rule_form.update_close_submit')}
            disabledSubmit={isManaged}
            centerCol={
              !create && (
                <Button type="button" bsStyle="info" disabled={isManaged} onClick={handleApply}>
                  {t('rule_form.update')}
                </Button>
              )
            }
            onCancel={handleCancel}
          />
        </Col>
      </Row>
    </form>
  );
};

export default RuleForm;
