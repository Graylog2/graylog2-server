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
import React, { useContext } from 'react';
import PropTypes from 'prop-types';

import { Button, Col, ControlLabel, FormControl, FormGroup, Row } from 'components/graylog';
import { SourceCodeEditor } from 'components/common';
import { Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import history from 'util/History';

import { PipelineRulesContext } from './RuleContext';
import PipelinesUsingRule from './PipelinesUsingRule';

const RuleForm = ({ create }) => {
  const {
    descriptionRef,
    handleDescription,
    handleSavePipelineRule,
    ruleSourceRef,
    onAceLoaded,
  } = useContext(PipelineRulesContext);

  const handleSubmit = (event) => {
    event.preventDefault();
    handleSavePipelineRule(() => { history.goBack(); });
  };

  const handleApply = () => {
    handleSavePipelineRule((rule) => { history.replace(Routes.SYSTEM.PIPELINES.RULE(rule.id)); });
  };

  const handleDescriptionChange = (event) => {
    handleDescription(event.target.value);
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

        <Input type="textarea"
               id="description"
               label="Description"
               onChange={handleDescriptionChange}
               autoFocus
               defaultValue={descriptionRef?.current?.value}
               help="Rule description (optional)."
               ref={descriptionRef} />

        <PipelinesUsingRule create={create} />

        <Input id="rule-source-editor" label="Rule source" help="Rule source, see quick reference for more information.">
          <SourceCodeEditor id={`source${create ? '-create' : '-edit'}`}
                            mode="pipeline"
                            onLoad={onAceLoaded}
                            innerRef={ruleSourceRef} />
        </Input>
      </fieldset>

      <Row>
        <Col md={12}>
          <div className="form-group">
            <Button type="submit" bsStyle="primary" style={{ marginRight: 10 }}>Save &amp; Close</Button>
            <Button type="button" bsStyle="info" style={{ marginRight: 10 }} onClick={handleApply}>Apply</Button>
            <Button type="button" onClick={handleCancel}>Cancel</Button>
          </div>
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
