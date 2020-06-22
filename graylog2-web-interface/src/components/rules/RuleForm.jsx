import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router';

import { Button, Col, ControlLabel, FormControl, FormGroup, Row } from 'components/graylog';
import { SourceCodeEditor } from 'components/common';
import { Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import history from 'util/History';

import RuleFormStyle from './RuleForm.css';

class RuleForm extends React.Component {
  static propTypes = {
    rule: PropTypes.object,
    usedInPipelines: PropTypes.array,
    create: PropTypes.bool,
    onSave: PropTypes.func.isRequired,
    validateRule: PropTypes.func.isRequired,
  };

  static defaultProps = {
    rule: {
      id: '',
      title: '',
      description: '',
      source: '',
    },
    usedInPipelines: [],
    create: false,
  };

  parseTimer = undefined;

  constructor(props) {
    super(props);
    const { rule } = props;

    this.state = {
      // when editing, take the rule that's been passed in
      rule: {
        id: rule.id,
        title: rule.title,
        description: rule.description,
        source: rule.source,
      },
      parseErrors: [],
    };
  }

  componentWillUnmount() {
    if (this.parseTimer !== undefined) {
      clearTimeout(this.parseTimer);
      this.parseTimer = undefined;
    }
  }

  _setParseErrors = (errors) => {
    this.setState({ parseErrors: errors });
  };

  _onSourceChange = (value) => {
    // don't try to parse the previous value, gets reset below
    if (this.parseTimer !== undefined) {
      clearTimeout(this.parseTimer);
    }
    const { rule } = this.state;
    const { validateRule } = this.props;

    rule.source = value;
    this.setState({ rule });

    if (validateRule) {
      // have the caller validate the rule after typing stopped for a while. usually this will mean send to server to parse
      this.parseTimer = setTimeout(() => validateRule(rule, this._setParseErrors), 500);
    }
  };

  _onDescriptionChange = (event) => {
    const { rule } = this.state;
    rule.description = event.target.value;
    this.setState({ rule });
  };

  _onTitleChange = (event) => {
    const { rule } = this.state;
    rule.title = event.target.value;
    this.setState({ rule });
  };

  _getId = (prefixIdName) => {
    const { name } = this.state;

    return name !== undefined ? prefixIdName + name : prefixIdName;
  };

  _goBack = () => {
    history.goBack();
  };

  _redirectToList = () => {
    history.push(Routes.SYSTEM.PIPELINES.RULES);
  };

  _save = (callback = () => {}) => {
    const { parseErrors, rule } = this.state;
    const { onSave } = this.props;

    if (parseErrors.length === 0) {
      onSave(rule, callback);
    }
  };

  _submit = (event) => {
    event.preventDefault();
    this._save(this._redirectToList);
  };

  _apply = () => {
    this._save();
  }

  _formatPipelinesUsingRule = () => {
    const { usedInPipelines } = this.props;

    if (usedInPipelines.length === 0) {
      return 'This rule is not being used in any pipelines.';
    }

    const formattedPipelines = usedInPipelines.map((pipeline) => {
      return (
        <li key={pipeline.id}>
          <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
            {pipeline.title}
          </Link>
        </li>
      );
    });

    return <ul className={RuleFormStyle.usedInPipelines}>{formattedPipelines}</ul>;
  };

  render() {
    const { parseErrors, rule } = this.state;
    const { create } = this.props;

    let pipelinesUsingRule;
    if (!create) {
      pipelinesUsingRule = (
        <Input id="used-in-pipelines" label="Used in pipelines" help="Pipelines that use this rule in one or more of their stages.">
          <div className="form-control-static">
            {this._formatPipelinesUsingRule()}
          </div>
        </Input>
      );
    }

    const annotations = parseErrors.map((e) => {
      return { row: e.line - 1, column: e.position_in_line - 1, text: e.reason, type: 'error' };
    });

    return (
      <form onSubmit={this._submit}>
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
                 value={rule.description} />

          {pipelinesUsingRule}

          <Input id="rule-source-editor" label="Rule source" help="Rule source, see quick reference for more information.">
            <SourceCodeEditor id={`source${create ? '-create' : '-edit'}`}
                              annotations={annotations}
                              value={rule.source}
                              onLoad={this._onLoad}
                              onChange={this._onSourceChange}
                              mode="pipeline" />
          </Input>
        </fieldset>

        <Row>
          <Col md={12}>
            <div className="form-group">
              <Button type="submit" bsStyle="primary" style={{ marginRight: 10 }}>Save & Close</Button>
              <Button type="button" bsStyle="info" style={{ marginRight: 10 }} onClick={this._apply}>Apply</Button>
              <Button type="button" onClick={this._goBack}>Cancel</Button>
            </div>
          </Col>
        </Row>
      </form>
    );
  }
}

export default RuleForm;
