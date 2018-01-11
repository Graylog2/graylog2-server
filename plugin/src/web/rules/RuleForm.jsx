import PropTypes from 'prop-types';
import React from 'react';
import { Button, Col, ControlLabel, FormControl, FormGroup, Row } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { SourceCodeEditor } from 'components/common';
import { Input } from 'components/bootstrap';
import Routes from 'routing/Routes';

import history from 'util/History';

import RuleFormStyle from './RuleForm.css';

const RuleForm = React.createClass({
  propTypes: {
    rule: PropTypes.object,
    usedInPipelines: PropTypes.array,
    create: PropTypes.bool,
    onSave: PropTypes.func.isRequired,
    validateRule: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      rule: {
        id: '',
        title: '',
        description: '',
        source: '',
      },
    };
  },

  getInitialState() {
    const rule = this.props.rule;
    return {
      // when editing, take the rule that's been passed in
      rule: {
        id: rule.id,
        title: rule.title,
        description: rule.description,
        source: rule.source,
      },
      parseErrors: [],
    };
  },

  componentWillUnmount() {
    if (this.parseTimer !== undefined) {
      clearTimeout(this.parseTimer);
      this.parseTimer = undefined;
    }
  },

  parseTimer: undefined,

  _setParseErrors(errors) {
    this.setState({ parseErrors: errors });
  },

  _onSourceChange(value) {
    // don't try to parse the previous value, gets reset below
    if (this.parseTimer !== undefined) {
      clearTimeout(this.parseTimer);
    }
    const rule = this.state.rule;
    rule.source = value;
    this.setState({ rule });

    if (this.props.validateRule) {
      // have the caller validate the rule after typing stopped for a while. usually this will mean send to server to parse
      this.parseTimer = setTimeout(() => this.props.validateRule(rule, this._setParseErrors), 500);
    }
  },

  _onDescriptionChange(event) {
    const rule = this.state.rule;
    rule.description = event.target.value;
    this.setState({ rule });
  },

  _onTitleChange(event) {
    const rule = this.state.rule;
    rule.title = event.target.value;
    this.setState({ rule });
  },

  _getId(prefixIdName) {
    return this.state.name !== undefined ? prefixIdName + this.state.name : prefixIdName;
  },

  _goBack() {
    history.goBack();
  },

  _saved() {
    history.push(Routes.pluginRoute('SYSTEM_PIPELINES_RULES'));
  },

  _save() {
    if (this.state.parseErrors.length === 0) {
      this.props.onSave(this.state.rule, this._saved);
    }
  },

  _submit(event) {
    event.preventDefault();
    this._save();
  },

  _formatPipelinesUsingRule() {
    if (this.props.usedInPipelines.length === 0) {
      return 'This rule is not being used in any pipelines.';
    }

    const formattedPipelines = this.props.usedInPipelines.map(pipeline => {
      return (
        <li key={pipeline.id}>
          <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_PIPELINEID')(pipeline.id)}>
            <a>{pipeline.title}</a>
          </LinkContainer>
        </li>
      );
    });

    return <ul className={RuleFormStyle.usedInPipelines}>{formattedPipelines}</ul>;
  },

  render() {
    let pipelinesUsingRule;
    if (!this.props.create) {
      pipelinesUsingRule = (
        <Input id="used-in-pipelines" label="Used in pipelines" help="Pipelines that use this rule in one or more of their stages.">
          <div className="form-control-static">
            {this._formatPipelinesUsingRule()}
          </div>
        </Input>
      );
    }

    const annotations = this.state.parseErrors.map(e => {
      return { row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' };
    });

    return (
      <form ref="form" onSubmit={this._submit}>
        <fieldset>
          <FormGroup id="ruleTitleInformation">
            <ControlLabel>Title</ControlLabel>
            <FormControl.Static>You can set the rule title in the rule source. See the quick reference for more information.</FormControl.Static>
          </FormGroup>

          <Input type="textarea"
                 id={this._getId('description')}
                 label="Description"
                 onChange={this._onDescriptionChange}
                 autoFocus
                 help="Rule description (optional)."
                 value={this.state.rule.description} />

          {pipelinesUsingRule}

          <Input id="rule-source-editor" label="Rule source" help="Rule source, see quick reference for more information.">
            <SourceCodeEditor id={`source${this.props.create ? '-create' : '-edit'}`}
                              annotations={annotations}
                              value={this.state.rule.source}
                              onLoad={this._onLoad}
                              onChange={this._onSourceChange} />
          </Input>
        </fieldset>

        <Row>
          <Col md={12}>
            <div className="form-group">
              <Button type="submit" bsStyle="primary" style={{ marginRight: 10 }}>Save</Button>
              <Button type="button" onClick={this._goBack}>Cancel</Button>
            </div>
          </Col>
        </Row>
      </form>
    );
  },
});

export default RuleForm;
