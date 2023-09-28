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
import type { SyntheticEvent } from 'react';
import React from 'react';
import cloneDeep from 'lodash/cloneDeep';
import get from 'lodash/get';
import styled from 'styled-components';

import { Select, SourceCodeEditor, TimezoneSelect, URLWhiteListInput } from 'components/common';
import { Button, Checkbox, Col, ControlLabel, FormGroup, HelpBlock, Input, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';
import type { HttpNotificationConfigV2, HttpNotificationValidationV2 } from 'components/event-notifications/types';

const StyledButton = styled(Button)`
  clear: both;
  display: block;
  margin-bottom: 15px;
`;

type Props = {
  config: HttpNotificationConfigV2,
  validation: HttpNotificationValidationV2
  onChange: any,
  setIsSubmitEnabled: any,
};

class HttpNotificationFormV2 extends React.Component<Props, any> {
  static defaultConfig = {
    url: '',
    api_key: '',
    api_secret: { keep_value: true },
    basic_auth: { keep_value: true },
    skip_tls_verification: false,
    method: 'POST',
    time_zone: 'UTC',
    body_template: '',
    content_type: 'JSON'
  };

  constructor(props: Props) {
    super(props);

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

  propagateChange = (key: string, value: any) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);

    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event: { target: { name: string }}) => {
    const { name } = event.target;
    const inputValue = FormsUtils.getValueFromInput(event.target);

    this.propagateChange(name, inputValue);
  };

  handleUrlChange = (event: SyntheticEvent<EventTarget>) => {
    const target = event.target as HTMLInputElement;
    this.propagateChange('url', target.value);
  };

  handleTimeZoneChange = (nextValue: string) => {
    this.propagateChange('time_zone', nextValue);
  };

  handleJsonBodyTemplateChange = (nextValue: string) => {
    this.propagateChange('body_template', nextValue);
  };

  handleMethodChange = (nextValue: string) => {
    if (nextValue === 'GET') {
      const { config, onChange } = this.props;
      const nextConfig = cloneDeep(config);

      nextConfig['method'] = nextValue;
      nextConfig['body_template'] = '';
      nextConfig['content_type'] = '';
      onChange(nextConfig);
    } else {
      this.propagateChange('method', nextValue);
    }
  };

  handleContentTypeChange = (nextValue: string) => {
      this.propagateChange('content_type', nextValue);
  };

  handleSecretInputChange = (event: { target: { name: string }}) => {
    const { name } = event.target;
    const inputValue = FormsUtils.getValueFromInput(event.target);
    const value = inputValue.length === 0 ? { delete_value: true } : { set_value: inputValue };

    this.setState({ [name]: inputValue });
    this.propagateChange(name, value);
  };

  onValidationChange = (validationState: string) => {
    const { setIsSubmitEnabled } = this.props;

    setIsSubmitEnabled(validationState !== 'error');
  };

  resetSecret = (attribute: string) => {
    const { reset } = this.state;
    reset[attribute] = true;
    this.setState({ reset });

    this.propagateChange(attribute, { delete_value: true });
    this.setState({ [attribute]: '' });
  };

  undoResetSecret = (attribute: string) => {
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

    const httpMethods = [{ value: 'POST', label: 'POST' }, { value: 'GET', label: 'GET' }, { value: 'PUT', label: 'PUT' }];
    const contentTypes = [{ value: 'JSON', label: 'application/json' }, { value: 'FORM_DATA', label: 'application/x-www-form-urlencoded' }, { value: 'PLAIN_TEXT', label: 'text/plain' }];
    const docsUrl = 'https://docs.graylog.org/docs/alerts#notifications';
    const helpElement = <p>Custom POST/PUT body. See <a href={docsUrl} rel="noopener noreferrer" target="_blank">docs </a>for more details. An empty POST/PUT body will send the full event details.</p>;

    return (
      <>
        <URLWhiteListInput label="URL"
                           onChange={this.handleUrlChange}
                           validationState={validation.errors.url ? 'error' : null}
                           validationMessage={get(validation, 'errors.url[0]', 'The URL to POST to when an Event occurs')}
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
                     help="The Basic authentication string needs to follow this format: '<username>:<password>'"
                     buttonAfter={reset.basic_auth ? (
                       <Button type="button" onClick={() => { this.undoResetSecret('basic_auth'); }}>
                         Undo Reset
                       </Button>
                     ) : undefined} />
            )}
          </Col>
        </Row>
        <Row>
          <Col md={6}>
            <Input id="api_key"
                   name="api_key"
                   label={<span>API Key <small className="text-muted">(Optional)</small></span>}
                   type="text"
                   onChange={this.handleChange}
                   bsStyle={validation.errors.api_key ? 'error' : null}
                   help={get(validation, 'errors.api_key[0]', 'Must be set if an API secret is set')}
                   value={config.api_key} />
          </Col>
          <Col md={6}>
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
                     help={get(validation, 'errors.api_secret[0]', 'Must be set if an API key is set')}
                     value={this.state.api_secret || ''}
                     buttonAfter={reset.api_secret ? (
                       <Button type="button" onClick={() => { this.undoResetSecret('api_secret'); }}>
                         Undo Reset
                       </Button>
                     ) : undefined} />
            )}
          </Col>
        </Row>
        <Row>
          <Col md={4}>
            <Input help="HTTP method used for the notification"
                   id="notification-method"
                   label="HTTP Method">
            <Select id="method"
                    name="method"
                    clearable={false}
                    options={httpMethods}
                    matchProp="label"
                    onChange={this.handleMethodChange}
                    value={config.method} />
            </Input>
          </Col>
          <Col md={4}>
            <Input help="HTTP content type used for POST/PUT notifications"
                   id="notification-content-type"
                   label="Content Type">
            <Select id="content-type"
                    name="content-type"
                    options={contentTypes}
                    matchProp="label"
                    disabled={config.method === 'GET'}
                    onChange={this.handleContentTypeChange}
                    value={config.content_type} />
            </Input>
          </Col>
          <Col md={4}>
            <Input id="notification-time-zone"
                   help="Time zone used for timestamps in the notification body"
                   label={<>Time zone for date/time values</>}>
              <TimezoneSelect className="timezone-select"
                              name="time_zone"
                              value={config.time_zone}
                              clearable={false}
                              onChange={this.handleTimeZoneChange} />
            </Input>
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <FormGroup controlId="notification-body-template"
                       validationState={validation.errors.body ? 'error' : null}>
              <ControlLabel>Body Template</ControlLabel>
              <SourceCodeEditor id="notification-body-template"
                                mode="text"
                                theme="light"
                                value={config.body_template || ''}
                                readOnly={config.method === 'GET'}
                                onChange={this.handleJsonBodyTemplateChange} />
              <HelpBlock>
                {get(validation, 'errors.body_template[0]', helpElement)}
              </HelpBlock>
            </FormGroup>
          </Col>
        </Row>
      </>
    );
  }
}

export default HttpNotificationFormV2;
