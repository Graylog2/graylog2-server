import PropTypes from 'prop-types';
import React from 'react';
import { Button, ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';

import { Input, InputWrapper } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

import EditableOnClickField from './EditableOnClickField';

const HTTPJSONPathAdapterFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    // eslint-disable-next-line react/no-unused-prop-types
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
    validationState: PropTypes.func.isRequired,
    validationMessage: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {};
  },
  _formatHeaders(headers) {
    if (headers) {
      return Object.keys(headers).map(key => (
        <p key={key} style={{ 'margin-bottom': 0 }}>
          <a onClick={() => this._onDeleteHeader(key)} style={{ marginRight: 5 }}>
            <i className="fa fa-trash-o" />
          </a>
          <EditableOnClickField value={key} onChange={newKey => this._updateHeaderKey(key, newKey)}>
            {key}
          </EditableOnClickField>:
          <EditableOnClickField value={headers[key]} onChange={newValue => this._updateHeaderValue(key, newValue)}>
            {headers[key]}
          </EditableOnClickField>
        </p>
      ));
    }
    return <span><i>Empty</i></span>;
  },
  _headerUpdated(headers) {
    const event = { target: { name: 'headers', value: headers } };
    this.props.handleFormEvent(event);
  },
  _onDeleteHeader(key) {
    if (window.confirm('Do you really want to delete this header?')) {
      const headers = this.props.config.headers || {};
      delete headers[key];
      this._headerUpdated(headers);
    }
  },
  _updateHeaderKey(oldKey, newKey) {
    const newHeaders = this.props.config.headers || {};
    newHeaders[newKey] = newHeaders[oldKey];
    delete newHeaders[oldKey];
    this._headerUpdated(newHeaders);
  },
  _updateHeaderValue(key, newValue) {
    const newHeaders = this.props.config.headers || {};
    newHeaders[key] = newValue;
    this._headerUpdated(newHeaders);
  },
  render() {
    const config = this.props.config;
    const onChangeHeaderKey = event => this.setState({ header_key: FormsUtils.getValueFromInput(event.target) });
    const onChangeHeaderValue = event => this.setState({ header_value: FormsUtils.getValueFromInput(event.target) });
    const onHeaderAdded = () => {
      if (!this.state.header_key || !this.state.header_value) {
        return;
      }
      const headers = config.headers || {};
      headers[this.state.header_key] = this.state.header_value;
      this._headerUpdated(headers);
      this.setState({ header_key: undefined, header_value: undefined });
    };
    const addHeaderForm = (
      <span>
        <Input type="text" id="key" name="key" label="Header"
               onChange={onChangeHeaderKey} value={this.state.header_key}
               labelClassName="col-sm-3" wrapperClassName="col-sm-5" />
        <Input type="text" id="value" name="value" label="Value"
               onChange={onChangeHeaderValue} value={this.state.header_value}
               labelClassName="col-sm-3" wrapperClassName="col-sm-5" />
        <Button className="pull-right" onClick={onHeaderAdded}>Add</Button>
      </span>
    );

    const httpHeaders = this._formatHeaders(config.headers);

    return (<fieldset>
      <Input type="text"
             id="url"
             name="url"
             label="Lookup URL"
             autoFocus
             required
             onChange={this.props.handleFormEvent}
             help={this.props.validationMessage('url', 'The URL for the lookup. (this is a template - see documentation)')}
             bsStyle={this.props.validationState('url')}
             value={config.url}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="single_value_jsonpath"
             name="single_value_jsonpath"
             label="Single value JSONPath"
             required
             onChange={this.props.handleFormEvent}
             help={this.props.validationMessage('single_value_jsonpath', 'The JSONPath string to get the single value from the response.')}
             bsStyle={this.props.validationState('single_value_jsonpath')}
             value={config.single_value_jsonpath}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="multi_value_jsonpath"
             name="multi_value_jsonpath"
             label="Multi value JSONPath"
             onChange={this.props.handleFormEvent}
             help={this.props.validationMessage('multi_value_jsonpath', 'The JSONPath string to get the multi value from the response. Needs to return a list or map. (optional)')}
             bsStyle={this.props.validationState('multi_value_jsonpath')}
             value={config.multi_value_jsonpath}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="user_agent"
             name="user_agent"
             label="HTTP User-Agent"
             required
             onChange={this.props.handleFormEvent}
             help="The User-Agent header to use for the HTTP request."
             value={config.user_agent}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <FormGroup>
        <ControlLabel className="col-sm-3">HTTP Headers</ControlLabel>
        <InputWrapper className="col-sm-9">
          <pre>
            {httpHeaders}
          </pre>
          <HelpBlock>The custom HTTP headers to use for the HTTP request. Multiple values must be comma-separated.</HelpBlock>
          {addHeaderForm}
        </InputWrapper>
      </FormGroup>

    </fieldset>);
  },
});

export default HTTPJSONPathAdapterFieldSet;
