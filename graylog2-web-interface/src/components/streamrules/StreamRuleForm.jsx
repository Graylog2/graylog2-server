import React from 'react';
import { Col } from 'react-bootstrap';
import LinkedStateMixin from 'react-addons-linked-state-mixin';

import { Input } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { TypeAheadFieldInput } from 'components/common';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import Version from 'util/Version';

import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';

const StreamRuleForm = React.createClass({
  propTypes: {
    onSubmit: React.PropTypes.func.isRequired,
    streamRule: React.PropTypes.object,
    streamRuleTypes: React.PropTypes.array.isRequired,
    title: React.PropTypes.string.isRequired,
  },
  mixins: [LinkedStateMixin],
  getDefaultProps() {
    return {
      streamRule: { field: '', type: 1, value: '', inverted: false, description: '' },
    };
  },
  getInitialState() {
    return this.props.streamRule;
  },
  FIELD_PRESENCE_RULE_TYPE: 5,
  ALWAYS_MATCH_RULE_TYPE: 7,
  _resetValues() {
    this.setState(this.props.streamRule);
  },
  _onSubmit() {
    if (this.state.type === this.ALWAYS_MATCH_RULE_TYPE) {
      this.state.field = '';
    }
    if (this.state.type === this.FIELD_PRESENCE_RULE_TYPE || this.state.type === this.ALWAYS_MATCH_RULE_TYPE) {
      this.state.value = '';
    }
    this.props.onSubmit(this.props.streamRule.id, this.state);
    this.refs.modal.close();
  },
  _formatStreamRuleType(streamRuleType) {
    return (
      <option key={`streamRuleType${streamRuleType.id}`}
              value={streamRuleType.id}>{streamRuleType.short_desc}</option>
    );
  },
  open() {
    this._resetValues();
    this.refs.modal.open();
  },
  close() {
    this.refs.modal.close();
  },
  render() {
    const streamRuleTypes = this.props.streamRuleTypes.map(this._formatStreamRuleType);
    const fieldBox = (String(this.state.type) !== String(this.ALWAYS_MATCH_RULE_TYPE) ?
      <TypeAheadFieldInput ref="fieldInput" type="text" required label="Field" valueLink={this.linkState('field')} autoFocus /> : '');
    const valueBox = (String(this.state.type) !== String(this.FIELD_PRESENCE_RULE_TYPE) && String(this.state.type) !== String(this.ALWAYS_MATCH_RULE_TYPE) ?
      <Input type="text" required label="Value" name="Value" valueLink={this.linkState('value')} /> : '');
    return (
      <BootstrapModalForm ref="modal"
                          title={this.props.title}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save">
        <div>
          <Col md={8}>
            {fieldBox}
            <Input type="select" required label="Type" name="Type" valueLink={this.linkState('type')}>
              {streamRuleTypes}
            </Input>
            {valueBox}
            <Input type="checkbox" label="Inverted" name="Inverted" checkedLink={this.linkState('inverted')} />

            <Input type="textarea" label="Description (optional)" name="Description" valueLink={this.linkState('description')} />

            <p>
              <strong>Result:</strong>
              {' '}
              <HumanReadableStreamRule streamRule={this.state} streamRuleTypes={this.props.streamRuleTypes} />
            </p>
          </Col>
          <Col md={4}>
            <div className="well well-sm matcher-github">
              The server will try to convert to strings or numbers based on the matcher type as good as it
              can.

              <br /><br />
              <i className="fa fa-github" />
              <a href={`https://github.com/Graylog2/graylog2-server/tree/${Version.getMajorAndMinorVersion()}/graylog2-server/src/main/java/org/graylog2/streams/matchers`}
                 target="_blank"> Take a look at the matcher code on GitHub
              </a>
              <br /><br />
              Regular expressions use Java syntax. <DocumentationLink page={DocsHelper.PAGES.STREAMS}
                                                                      title="More information"
                                                                      text={<i className="fa fa-lightbulb-o" />} />
            </div>
          </Col>
        </div>
      </BootstrapModalForm>
    );
  },
});

export default StreamRuleForm;
