import React, { PropTypes } from 'react';

import { Input } from 'components/bootstrap';

const HTTPJSONPathAdapterFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    // eslint-disable-next-line react/no-unused-prop-types
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
  },

  render() {
    const config = this.props.config;

    return (<fieldset>
      <Input type="text"
             id="url"
             name="url"
             label="Lookup URL"
             autoFocus
             required
             onChange={this.props.handleFormEvent}
             help="The URL for the lookup. (this is a template - see documentation)"
             value={config.url}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="single_value_jsonpath"
             name="single_value_jsonpath"
             label="Single value JSONPath"
             required
             onChange={this.props.handleFormEvent}
             help="The JSONPath string to get the single value from the response."
             value={config.single_value_jsonpath}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="multi_value_jsonpath"
             name="multi_value_jsonpath"
             label="Multi value JSONPath"
             onChange={this.props.handleFormEvent}
             help={<span>The JSONPath string to get the multi value from the response. Needs to return a list or map. <strong>(optional)</strong></span>}
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
    </fieldset>);
  },
});

export default HTTPJSONPathAdapterFieldSet;
