import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';
import URLWhitelistInput from 'components/configurations/URLWhitelistInput';
import { KeyValueTable } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

class HTTPJSONPathAdapterFieldSet extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    // eslint-disable-next-line react/no-unused-prop-types
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
    validationState: PropTypes.func.isRequired,
    validationMessage: PropTypes.func.isRequired,
  };

  state = {};

  onHTTPHeaderUpdate = (headers) => {
    const { config, updateConfig } = this.props;
    const configChange = ObjectUtils.clone(config);
    configChange.headers = headers;
    updateConfig(config);
  };

  render() {
    const { config, handleFormEvent, validationMessage, validationState } = this.props;

    return (
      <fieldset>
        <URLWhitelistInput label="Lookup URL"
                           onChange={handleFormEvent}
                           validationMessage={validationMessage('url', 'The URL for the lookup. (this is a template - see documentation)')}
                           validationState={validationState('url')}
                           url={config.url}
                           labelClassName="col-sm-3"
                           wrapperClassName="col-sm-9"
                           urlType="regex" />
        <Input type="text"
               id="single_value_jsonpath"
               name="single_value_jsonpath"
               label="Single value JSONPath"
               required
               onChange={handleFormEvent}
               help={validationMessage('single_value_jsonpath', 'The JSONPath string to get the single value from the response.')}
               bsStyle={validationState('single_value_jsonpath')}
               value={config.single_value_jsonpath}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="text"
               id="multi_value_jsonpath"
               name="multi_value_jsonpath"
               label="Multi value JSONPath"
               onChange={handleFormEvent}
               help={validationMessage('multi_value_jsonpath', 'The JSONPath string to get the multi value from the response. Needs to return a list or map. (optional)')}
               bsStyle={validationState('multi_value_jsonpath')}
               value={config.multi_value_jsonpath}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="text"
               id="user_agent"
               name="user_agent"
               label="HTTP User-Agent"
               required
               onChange={handleFormEvent}
               help="The User-Agent header to use for the HTTP request."
               value={config.user_agent}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input id="http_headers"
               label="HTTP Headers"
               help="The custom HTTP headers to use for the HTTP request. Multiple values must be comma-separated."
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <KeyValueTable pairs={config.headers || {}} editable onChange={this.onHTTPHeaderUpdate} />
        </Input>

      </fieldset>
    );
  }
}

export default HTTPJSONPathAdapterFieldSet;
