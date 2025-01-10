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
import styled from 'styled-components';

import { Button, Col, ControlLabel, FormControl, FormGroup, Row, Input } from 'components/bootstrap';
import { ConfirmLeaveDialog, SourceCodeEditor, FormSubmit } from 'components/common';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';

import { PipelineRulesContext } from './RuleContext';
import PipelinesUsingRule from './PipelinesUsingRule';
import RuleSimulation from './RuleSimulation';

type Props = {
  create?: boolean
};

const StyledContainer = styled.div`
  & .source-code-editor div {
    border-color: ${({ theme }) => theme.colors.input.border};
    border-radius: 0;

    & .ace_editor {
      border-radius: 0;
    }
  }

  & .ace_tooltip.ace-graylog {
    background-color: ${({ theme }) => theme.colors.global.background};
    padding: 4px;
    padding-left: 0;
    line-height: 1.5;
  }
`;

const RuleForm = ({ create = false }: Props) => {
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
      setErrorMessage(`Rule title "${duplicatedTitle}" already exists!`);
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
               rows={1}
               help="Rule description (optional)." />

        <PipelinesUsingRule create={create} />

        <Input id="rule-source-editor" label="Rule source" help="Rule source, see quick reference for more information." error={errorMessage}>
          <StyledContainer>
            <SourceCodeEditor id={`source${create ? '-create' : '-edit'}`}
                              mode="pipeline"
                              onLoad={onAceLoaded}
                              onChange={handleSourceChange}
                              value={ruleSource}
                              innerRef={ruleSourceRef} />
          </StyledContainer>
        </Input>

        <RuleSimulation />
      </fieldset>

      <Row>
        <Col md={12}>
          <FormSubmit submitButtonText={create ? 'Create rule' : 'Update rule & close'}
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

export default RuleForm;
