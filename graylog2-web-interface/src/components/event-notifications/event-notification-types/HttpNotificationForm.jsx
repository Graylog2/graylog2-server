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
import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { URLWhiteListInput } from 'components/common';
import { Button, Col, Input, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

class HttpNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    setIsSubmitEnabled: PropTypes.func,
  };

  static defaultConfig = {
    url: '',
    api_key: '',
    api_secret: { keep_value: true },
    basic_auth: { keep_value: true },
  };

  constructor() {
    super();

    this.state = {
      api_secret: '',
      basic_auth: '',
    };
  }

  componentDidMount() {
    const { config, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(config);

    nextConfig.basic_auth = config.basic_auth?.is_set ? { keep_value: true } : null;
    nextConfig.api_secret = config.api_secret?.is_set ? { keep_value: true } : null;

    onChange(nextConfig);

    this.setState({ basic_auth: config.basic_auth.is_set ? '******' : '' });
    this.setState({ api_secret: config.api_secret.is_set ? '******' : '' });
  }

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(config);

    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event) => {
    const { name } = event.target;
    const inputValue = FormsUtils.getValueFromInput(event.target);

    this.propagateChange(name, inputValue);
  };

  handleSecretInputChange = (event) => {
    const { name } = event.target;
    const inputValue = FormsUtils.getValueFromInput(event.target);
    const value = inputValue.length === 0 ? { delete_value: true } : { set_value: inputValue };

    this.setState({ [name]: inputValue });
    this.propagateChange(name, value);
  };

  onValidationChange = (validationState) => {
    const { setIsSubmitEnabled } = this.props;

    setIsSubmitEnabled(validationState !== 'error');
  };

  resetSecret = (attribute) => {
    this.propagateChange(attribute, { delete_value: true });
    this.setState({ [attribute]: '' });
  };

  undoResetSecret = (attribute) => {
    this.propagateChange(attribute, { keep_value: true });
    this.setState({ [attribute]: '******' });
  };

  render() {
    const { config, validation } = this.props;
    const { api_secret, basic_auth } = config;
    const basicAuthColumnSize = basic_auth?.keep_value || basic_auth?.delete_value ? 10 : 12;
    const apiSecretColumnSize = api_secret?.keep_value || api_secret?.delete_value ? 10 : 12;

    return (
      <>
        <URLWhiteListInput label="URL"
                           onChange={this.handleChange}
                           validationState={validation.errors.url ? 'error' : null}
                           validationMessage={lodash.get(validation, 'errors.url[0]', 'The URL to POST to when an Event occurs.')}
                           onValidationChange={this.onValidationChange}
                           url={config.url} />
        <Row>
          <Col md={basicAuthColumnSize}>
            <Input id="basicAuth"
                   onChange={this.handleSecretInputChange}
                   label="Basic Authentication"
                   type="password"
                   name="basic_auth"
                   value={this.state.basic_auth || ''} />
          </Col>
          {basic_auth?.keep_value === true && (
            <Col md={2}>
              <Button bsStyle="danger" type="button" onClick={() => { this.resetSecret('basic_auth'); }}>
                Reset Secret
              </Button>
            </Col>
          )}
          {basic_auth?.delete_value === true && (
            <Col md={2}>
              <Button type="button" onClick={() => { this.undoResetSecret('basic_auth'); }}>
                Undo Reset
              </Button>
            </Col>
          )}
        </Row>
        <Input id="api_key"
               name="api_key"
               label="API Key"
               type="text"
               onChange={this.handleChange}
               bsStyle={validation.errors.api_key ? 'error' : null}
               help={lodash.get(validation, 'errors.api_key[0]', 'The API Key.')}
               value={config.api_key} />
        <Row>
          <Col md={apiSecretColumnSize}>
            <Input id="api_secret"
                   name="api_secret"
                   label="API Secret"
                   type="password"
                   onChange={this.handleSecretInputChange}
                   bsStyle={validation.errors.api_secret ? 'error' : null}
                   help={lodash.get(validation, 'errors.api_secret[0]', 'The API Secret')}
                   value={this.state.api_secret || ''} />
          </Col>
          {api_secret?.keep_value === true && (
            <Col md={2}>
              <Button bsStyle="danger" type="button" onClick={() => { this.resetSecret('api_secret'); }}>
                Reset Secret
              </Button>
            </Col>
          )}
          {api_secret?.delete_value === true && (
            <Col md={2}>
              <Button type="button" onClick={() => { this.undoResetSecret('api_secret'); }}>
                Undo Reset
              </Button>
            </Col>
          )}
        </Row>
      </>
    );
  }
}

HttpNotificationForm.defaultProps = {
  setIsSubmitEnabled: () => {},
};

export default HttpNotificationForm;
