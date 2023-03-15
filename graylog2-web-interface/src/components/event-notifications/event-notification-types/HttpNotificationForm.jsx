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
import cloneDeep from 'lodash/cloneDeep';
import get from 'lodash/get';
import styled from 'styled-components';

import { URLWhiteListInput } from 'components/common';
import { Button, Checkbox, Col, ControlLabel, Input, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

const StyledButton = styled(Button)`
  clear: both;
  display: block;
  margin-bottom: 15px;
`;

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
    skip_tls_verification: false,
  };

  constructor() {
    super();

    this.state = {
      api_secret: '',
      basic_auth: '',
      reset: {
        api_secret: false,
        basic_auth: false,
      },
    };
  }

  componentDidMount() {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);

    nextConfig.basic_auth = config.basic_auth?.is_set ? { keep_value: true } : null;
    nextConfig.api_secret = config.api_secret?.is_set ? { keep_value: true } : null;

    onChange(nextConfig);

    this.setState({ basic_auth: config.basic_auth.is_set ? '******' : '' });
    this.setState({ api_secret: config.api_secret.is_set ? '******' : '' });
  }

  propagateChange = (key, value) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);

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
    const { reset } = this.state;
    reset[attribute] = true;
    this.setState({ reset });

    this.propagateChange(attribute, { delete_value: true });
    this.setState({ [attribute]: '' });
  };

  undoResetSecret = (attribute) => {
    const { reset } = this.state;
    reset[attribute] = false;
    this.setState({ reset });

    this.propagateChange(attribute, { keep_value: true });
    this.setState({ [attribute]: '******' });
  };

  render() {
    const { config, validation } = this.props;
    const { api_secret, basic_auth } = config;
    const { reset } = this.state;

    return (
      <>
        <URLWhiteListInput label="URL"
                           onChange={this.handleChange}
                           validationState={validation.errors.url ? 'error' : null}
                           validationMessage={get(validation, 'errors.url[0]', 'The URL to POST to when an Event occurs.')}
                           onValidationChange={this.onValidationChange}
                           url={config.url}
                           autofocus={false} />
        <Checkbox id="skip_tls_verification"
                  name="skip_tls_verification"
                  onChange={this.handleChange}
                  checked={config.skip_tls_verification}>
          Skip TLS verification
        </Checkbox>
        <Row>
          <Col md={12}>
            {basic_auth?.keep_value ? (
              <>
                <ControlLabel>Basic authentication</ControlLabel>
                <StyledButton bsStyle="default" type="button" onClick={() => { this.resetSecret('basic_auth'); }}>
                  Reset Secret
                </StyledButton>
              </>
            ) : (
              <Input id="basicAuth"
                     label={<span>Basic authentication <small className="text-muted">(Optional)</small></span>}
                     name="basic_auth"
                     type="password"
                     onChange={this.handleSecretInputChange}
                     value={this.state.basic_auth || ''}
                     help="The Basic authentication string needs to follow this format: '<username>:<password>'."
                     buttonAfter={reset.basic_auth ? (
                       <Button type="button" onClick={() => { this.undoResetSecret('basic_auth'); }}>
                         Undo Reset
                       </Button>
                     ) : undefined} />
            )}
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <Input id="api_key"
                   name="api_key"
                   label={<span>API Key <small className="text-muted">(Optional)</small></span>}
                   type="text"
                   onChange={this.handleChange}
                   bsStyle={validation.errors.api_key ? 'error' : null}
                   help={get(validation, 'errors.api_key[0]', 'If an API secret is set, an API key must also be set.')}
                   value={config.api_key} />
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            {api_secret?.keep_value ? (
              <>
                <ControlLabel>API Secret</ControlLabel>
                <StyledButton bsStyle="default" type="button" onClick={() => { this.resetSecret('api_secret'); }}>
                  Reset Secret
                </StyledButton>
              </>
            ) : (
              <Input id="apiSecret"
                     label={<span>API Secret <small className="text-muted">(Optional)</small></span>}
                     name="api_secret"
                     type="password"
                     onChange={this.handleSecretInputChange}
                     bsStyle={validation.errors.api_secret ? 'error' : null}
                     help={get(validation, 'errors.api_secret[0]', 'If an API key is set, an API secret must also be set.')}
                     value={this.state.api_secret || ''}
                     buttonAfter={reset.api_secret ? (
                       <Button type="button" onClick={() => { this.undoResetSecret('api_secret'); }}>
                         Undo Reset
                       </Button>
                     ) : undefined} />
            )}
          </Col>
        </Row>
      </>
    );
  }
}

HttpNotificationForm.defaultProps = {
  setIsSubmitEnabled: () => {},
};

export default HttpNotificationForm;
