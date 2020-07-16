// @flow strict
import PropTypes from 'prop-types';
import React from 'react';

import { Col, Well } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { Icon, TypeAheadFieldInput } from 'components/common';
import { DocumentationLink } from 'components/support';
import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import DocsHelper from 'util/DocsHelper';
import Version from 'util/Version';
import FormsUtils from 'util/FormsUtils';

const { InputsStore, InputsActions } = CombinedProvider.get('Inputs');

type StreamRule = {
  type: number,
  field: string,
  value: string,
  id?: string,
  inverted: boolean,
  description: string,
};

type StreamRuleType = {
  id: number,
  short_desc: string,
  long_desc: string,
};

type Props = {
  onSubmit: (?string, StreamRule) => void,
  streamRule: StreamRule,
  streamRuleTypes: [StreamRuleType],
  title: string,
  inputs: [],
  onClose: () => void,
};

type State = {
  streamRule: StreamRule,
  error: string,
};

class StreamRuleForm extends React.Component<Props, State> {
  static defaultProps = {
    streamRule: { field: '', type: 1, value: '', inverted: false, description: '' },
    inputs: [],
    onClose: () => {},
  };

  FIELD_PRESENCE_RULE_TYPE = 5;

  ALWAYS_MATCH_RULE_TYPE = 7;

  MATCH_INPUT = 8;

  PLACEHOLDER_INPUT = 0;

  constructor(props) {
    super(props);

    this.state = {
      streamRule: props.streamRule,
      error: '',
    };
  }

  componentDidMount() {
    InputsActions.list();
  }

  _validateMatchInput = () => {
    const { streamRule: { value } } = this.state;

    if (String(value) === String(this.PLACEHOLDER_INPUT)) {
      this.setState({ error: 'Please choose an input' });

      return false;
    }

    this.setState({ error: '' });

    return true;
  };

  _onSubmit = () => {
    const { streamRule: currentStreamRule } = this.state;
    const { type } = currentStreamRule;
    const { streamRule, onSubmit } = this.props;
    const { onClose } = this.props;

    if (type === this.ALWAYS_MATCH_RULE_TYPE) {
      currentStreamRule.field = '';
      this.setState({ streamRule: currentStreamRule });
    }

    if (type === this.FIELD_PRESENCE_RULE_TYPE || type === this.ALWAYS_MATCH_RULE_TYPE) {
      currentStreamRule.value = '';
      this.setState({ streamRule: currentStreamRule });
    }

    if (String(type) === String(this.MATCH_INPUT)) {
      if (!this._validateMatchInput()) {
        return;
      }
    }

    onSubmit(streamRule.id, currentStreamRule);
    onClose();
  };

  _formatStreamRuleType = (streamRuleType) => (
    <option key={`streamRuleType${streamRuleType.id}`}
            value={streamRuleType.id}>
      {streamRuleType.short_desc}
    </option>
  );

  handleChange = (event) => {
    const { streamRule } = this.state;

    streamRule[event.target.name] = FormsUtils.getValueFromInput(event.target);

    if (event.target.name === 'type' && String(streamRule.type) === String(this.MATCH_INPUT)) {
      streamRule.value = String(this.PLACEHOLDER_INPUT);
    }

    this.setState({ streamRule });
  };

  _formatInputOptions = (input) => (
    <option key={`input-${input.id}`} value={input.id}>
      {input.title} ({input.name})
    </option>
  );

  valueBox = () => {
    const { streamRule: { value, type }, error } = this.state;
    const { inputs } = this.props;

    switch (String(type)) {
      case String(this.FIELD_PRESENCE_RULE_TYPE):
      case String(this.ALWAYS_MATCH_RULE_TYPE):
        return '';

      case String(this.MATCH_INPUT): {
        const bsStyle = error && error.length > 0 ? 'error' : undefined;

        return (
          <Input bsStyle={bsStyle}
                 help={error}
                 id="Value"
                 type="select"
                 required
                 label="Value"
                 name="value"
                 value={value}
                 data-testid="input-selection"
                 onChange={this.handleChange}>
            <option value={this.PLACEHOLDER_INPUT}>Choose Input</option>
            {inputs.map(this._formatInputOptions)}
          </Input>
        );
      }

      default:
        return <Input id="Value" type="text" required label="Value" name="value" value={value} onChange={this.handleChange} />;
    }
  };

  fieldBox = () => {
    const { streamRule: { field, type } } = this.state;

    switch (String(type)) {
      case String(this.ALWAYS_MATCH_RULE_TYPE):
      case String(this.MATCH_INPUT):
        return '';
      default:
        return <TypeAheadFieldInput id="field-input" type="text" required label="Field" name="field" defaultValue={field} onChange={this.handleChange} autoFocus />;
    }
  };

  render() {
    const { streamRule } = this.state;
    const { type, inverted, description } = streamRule;
    const { streamRuleTypes: ruleTypes, title, onClose, inputs } = this.props;

    const streamRuleTypes = ruleTypes.map(this._formatStreamRuleType);
    const fieldBox = this.fieldBox();
    const valueBox = this.valueBox();

    return (
      <BootstrapModalForm title={title}
                          show
                          onCancel={onClose}
                          onModalClose={onClose}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save"
                          formProps={{ id: 'StreamRuleForm' }}>
        <div>
          <Col md={8}>
            {fieldBox}
            <Input id="Type" data-testid="rule-type-selection" type="select" required label="Type" name="type" value={type} onChange={this.handleChange}>
              {streamRuleTypes}
            </Input>
            {valueBox}
            <Input id="Inverted" type="checkbox" label="Inverted" name="inverted" checked={inverted} onChange={this.handleChange} />

            <Input id="Description" type="textarea" label="Description (optional)" name="description" value={description} onChange={this.handleChange} />

            <p>
              <strong>Result:</strong>
              {' '}
              <HumanReadableStreamRule streamRule={streamRule} streamRuleTypes={ruleTypes} inputs={inputs} />
            </p>
          </Col>
          <Col md={4}>
            <Well bsSize="small" className="matcher-github">
              The server will try to convert to strings or numbers based on the matcher type as well as it can.

              <br /><br />
              <Icon name="github" type="brand" />&nbsp;
              <a href={`https://github.com/Graylog2/graylog2-server/tree/${Version.getMajorAndMinorVersion()}/graylog2-server/src/main/java/org/graylog2/streams/matchers`}
                 target="_blank"
                 rel="noopener noreferrer"> Take a look at the matcher code on GitHub
              </a>
              <br /><br />
              Regular expressions use Java syntax. <DocumentationLink page={DocsHelper.PAGES.STREAMS}
                                                                      title="More information"
                                                                      text={<Icon name="lightbulb" type="regular" />} />
            </Well>
          </Col>
        </div>
      </BootstrapModalForm>
    );
  }
}

StreamRuleForm.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  streamRule: PropTypes.object,
  streamRuleTypes: PropTypes.array.isRequired,
  title: PropTypes.string.isRequired,
  inputs: PropTypes.array,
  onClose: PropTypes.func,
};

export default connect(StreamRuleForm,
  { inputs: InputsStore },
  ({ inputs }) => ({ inputs: inputs.inputs }));
