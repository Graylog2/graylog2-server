import PropTypes from 'prop-types';
import React from 'react';

import { Col, Well } from 'components/graylog';
import { Input, BootstrapModalForm } from 'components/bootstrap';
import { TypeAheadFieldInput, Icon } from 'components/common';
import { DocumentationLink } from 'components/support';
import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';

import DocsHelper from 'util/DocsHelper';
import Version from 'util/Version';
import FormsUtils from 'util/FormsUtils';

class StreamRuleForm extends React.Component {
  FIELD_PRESENCE_RULE_TYPE = 5;

  ALWAYS_MATCH_RULE_TYPE = 7;

  static propTypes = {
    onSubmit: PropTypes.func.isRequired,
    streamRule: PropTypes.object,
    streamRuleTypes: PropTypes.array.isRequired,
    title: PropTypes.string.isRequired,
  };

  static defaultProps = {
    streamRule: { field: '', type: 1, value: '', inverted: false, description: '' },
  };

  constructor(props) {
    super(props);

    this.state = props.streamRule;
    this.modal = undefined;
  }

  _resetValues = () => {
    const { streamRule } = this.props;

    this.setState(streamRule);
  };

  _onSubmit = () => {
    const { type } = this.state;
    const { streamRule, onSubmit } = this.props;

    if (type === this.ALWAYS_MATCH_RULE_TYPE) {
      this.setState({ field: '' });
    }
    if (type === this.FIELD_PRESENCE_RULE_TYPE || type === this.ALWAYS_MATCH_RULE_TYPE) {
      this.setState({ value: '' });
    }
    onSubmit(streamRule.id, this.state);
    this.modal.close();
  };

  _formatStreamRuleType = (streamRuleType) => {
    return (
      <option key={`streamRuleType${streamRuleType.id}`}
              value={streamRuleType.id}>
        {streamRuleType.short_desc}
      </option>
    );
  };

  open = () => {
    this._resetValues();
    this.modal.open();
  };

  close = () => {
    this.modal.close();
  };

  handleChange = (event) => {
    const change = {};
    change[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState(change);
  };

  render() {
    const { field, type, value, inverted, description } = this.state;
    const { streamRuleTypes: ruleTypes, title } = this.props;

    const streamRuleTypes = ruleTypes.map(this._formatStreamRuleType);
    const fieldBox = (String(type) !== String(this.ALWAYS_MATCH_RULE_TYPE)
      ? <TypeAheadFieldInput id="field-input" type="text" required label="Field" name="field" defaultValue={field} onChange={this.handleChange} autoFocus /> : '');
    const valueBox = (String(type) !== String(this.FIELD_PRESENCE_RULE_TYPE) && String(type) !== String(this.ALWAYS_MATCH_RULE_TYPE)
      ? <Input id="Value" type="text" required label="Value" name="value" value={value} onChange={this.handleChange} /> : '');
    return (
      <BootstrapModalForm ref={(c) => { this.modal = c; }}
                          title={title}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save"
                          formProps={{ id: 'StreamRuleForm' }}>
        <div>
          <Col md={8}>
            {fieldBox}
            <Input id="Type" type="select" required label="Type" name="type" value={type} onChange={this.handleChange}>
              {streamRuleTypes}
            </Input>
            {valueBox}
            <Input id="Inverted" type="checkbox" label="Inverted" name="inverted" checked={inverted} onChange={this.handleChange} />

            <Input id="Description" type="textarea" label="Description (optional)" name="description" value={description} onChange={this.handleChange} />

            <p>
              <strong>Result:</strong>
              {' '}
              <HumanReadableStreamRule streamRule={this.state} streamRuleTypes={ruleTypes} />
            </p>
          </Col>
          <Col md={4}>
            <Well bsSize="small" className="matcher-github">
              The server will try to convert to strings or numbers based on the matcher type as well as it can.

              <br /><br />
              <Icon name={{ prefix: 'fab', iconName: 'github' }} />
              <a href={`https://github.com/Graylog2/graylog2-server/tree/${Version.getMajorAndMinorVersion()}/graylog2-server/src/main/java/org/graylog2/streams/matchers`}
                 target="_blank"
                 rel="noopener noreferrer"> Take a look at the matcher code on GitHub
              </a>
              <br /><br />
              Regular expressions use Java syntax. <DocumentationLink page={DocsHelper.PAGES.STREAMS}
                                                                      title="More information"
                                                                      text={<Icon name="lightbulb-o" />} />
            </Well>
          </Col>
        </div>
      </BootstrapModalForm>
    );
  }
}

export default StreamRuleForm;
