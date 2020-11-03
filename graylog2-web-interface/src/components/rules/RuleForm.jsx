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
  } = useContext(PipelineRulesContext);

  const handleSubmit = (event) => {
    event.preventDefault();
    handleSavePipelineRule(() => { history.push(Routes.SYSTEM.PIPELINES.RULES); });
  };

  const handleApply = () => {
    handleSavePipelineRule((rule) => { history.push(Routes.SYSTEM.PIPELINES.RULE(rule.id)); });
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
