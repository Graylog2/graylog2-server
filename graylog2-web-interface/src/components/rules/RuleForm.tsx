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
import PropTypes from 'prop-types';

import { Button, Col, ControlLabel, FormControl, FormGroup, Row, Input } from 'components/bootstrap';
import { ConfirmLeaveDialog, SourceCodeEditor, FormSubmit } from 'components/common';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

import { PipelineRulesContext } from './RuleContext';
import PipelinesUsingRule from './PipelinesUsingRule';
import RuleSimulation from './RuleSimulation';

type Props = {
  create: boolean,
};

const RuleForm = ({ create }: Props) => {
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

  const handleError = (error) => {
    if (error.responseMessage.includes('duplicate key error')) {
      const duplicatedTitle = error.responseMessage.match(/title: "(.*)"/i)[1];
      setErrorMessage(`Rule title "${duplicatedTitle}" already exists!`);
    }
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    handleSavePipelineRule(() => {
      setErrorMessage('');
      setIsDirty(false);
      history.goBack();
    }, handleError);
  };

  const handleApply = () => {
    handleSavePipelineRule((rule) => {
      setErrorMessage('');
      setIsDirty(false);
      history.replace(Routes.SYSTEM.PIPELINES.RULE(rule.id));
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

  const handleCancel = () => {
    history.goBack();
  };

  return (
    <form onSubmit={handleSubmit}>
      <fieldset>
        <FormGroup id="ruleTitleInformation">
          <ControlLabel>Title</ControlLabel>
          <FormControl.Static>You can set the rule title in the rule source. See the quick reference for more information.</FormControl.Static>
        </FormGroup>

        {isDirty && (
          <ConfirmLeaveDialog question="Do you really want to abandon this page and lose your changes? This action cannot be undone." />
        )}

        <Input type="textarea"
               id="description"
               label="Description"
               value={description}
               onChange={handleDescriptionChange}
               autoFocus
               help="Rule description (optional)." />

        <PipelinesUsingRule create={create} />

        <Input id="rule-source-editor" label="Rule source" help="Rule source, see quick reference for more information." error={errorMessage}>
          {/* TODO: Figure out issue with props */}
          {/* @ts-ignore */}
          <SourceCodeEditor id={`source${create ? '-create' : '-edit'}`}
                            mode="pipeline"
                            onLoad={onAceLoaded}
                            onChange={handleSourceChange}
                            value={ruleSource}
                            innerRef={ruleSourceRef} />
        </Input>

        <RuleSimulation />
      </fieldset>

      <Row>
        <Col md={12}>
          <FormSubmit submitButtonText={`${create ? 'Create rule' : 'Update rule & close'}`}
                      centerCol={!create && (
                        <Button type="button" bsStyle="info" onClick={handleApply}>
                          Update rule
                        </Button>
                      )}
                      onCancel={handleCancel} />
        </Col>
      </Row>
    </form>
  );
};

RuleForm.propTypes = {
  create: PropTypes.bool,
};

RuleForm.defaultProps = {
  create: false,
};

export default RuleForm;
