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
import cloneDeep from 'lodash/cloneDeep';
// eslint-disable-next-line no-restricted-imports
import get from 'lodash/get';
import styled from 'styled-components';

import i18n from 'i18n';
import { URLAllowListInput } from 'components/common';
import { Button, Checkbox, Col, ControlLabel, Input, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';
import type { EventNotificationTypes } from 'components/event-notifications/types';

const StyledButton = styled(Button)`
  clear: both;
  display: block;
  margin-bottom: 15px;
`;

type Props = React.ComponentProps<EventNotificationTypes['formComponent']>;

// The form is a class component, so texts are translated at render time.
const t = (key: string) => i18n.t(key, { ns: 'event_notifications' });

class HttpNotificationForm extends React.Component<
  Props,
  {
    [key: string]: any;
  }
> {
  constructor(props) {
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

  static defaultConfig = {
    url: '',
    api_key_as_header: false,
    api_key: '',
    api_secret: { keep_value: true },
    basic_auth: { keep_value: true },
    skip_tls_verification: false,
  };

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
    const inputValue = String(FormsUtils.getValueFromInput(event.target));
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
        <URLAllowListInput
          label={t('http.url')}
          onChange={this.handleChange}
          validationState={validation.errors.url ? 'error' : null}
          validationMessage={get(validation, 'errors.url[0]', t('http.url_help'))}
          onValidationChange={this.onValidationChange}
          url={config.url}
          autofocus={false}
        />
        <Checkbox
          id="skip_tls_verification"
          name="skip_tls_verification"
          onChange={this.handleChange}
          checked={config.skip_tls_verification}>
          {t('http.skip_tls_verification')}
        </Checkbox>
        <Row>
          <Col md={12}>
            {basic_auth?.keep_value ? (
              <>
                <ControlLabel>{t('http.basic_auth')}</ControlLabel>
                <StyledButton
                  bsStyle="default"
                  type="button"
                  onClick={() => {
                    this.resetSecret('basic_auth');
                  }}>
                  {t('http.reset_secret')}
                </StyledButton>
              </>
            ) : (
              <Input
                id="basicAuth"
                label={
                  <span>
                    {t('http.basic_auth')} <small className="text-muted">{t('form.optional')}</small>
                  </span>
                }
                name="basic_auth"
                type="password"
                onChange={this.handleSecretInputChange}
                value={this.state.basic_auth || ''}
                help={t('http.basic_auth_help')}
                buttonAfter={
                  reset.basic_auth ? (
                    <Button
                      type="button"
                      onClick={() => {
                        this.undoResetSecret('basic_auth');
                      }}>
                      {t('http.undo_reset')}
                    </Button>
                  ) : undefined
                }
              />
            )}
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <Input
              id="api_key"
              name="api_key"
              label={
                <span>
                  {t('http.api_key')} <small className="text-muted">{t('form.optional')}</small>
                </span>
              }
              type="text"
              onChange={this.handleChange}
              bsStyle={validation.errors.api_key ? 'error' : null}
              help={get(validation, 'errors.api_key[0]', t('http.api_key_help'))}
              value={config.api_key}
            />
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            {api_secret?.keep_value ? (
              <>
                <ControlLabel>{t('http.api_secret')}</ControlLabel>
                <StyledButton
                  bsStyle="default"
                  type="button"
                  onClick={() => {
                    this.resetSecret('api_secret');
                  }}>
                  {t('http.reset_secret')}
                </StyledButton>
              </>
            ) : (
              <Input
                id="apiSecret"
                label={
                  <span>
                    {t('http.api_secret')} <small className="text-muted">{t('form.optional')}</small>
                  </span>
                }
                name="api_secret"
                type="password"
                onChange={this.handleSecretInputChange}
                bsStyle={validation.errors.api_secret ? 'error' : null}
                help={get(validation, 'errors.api_secret[0]', t('http.api_secret_help'))}
                value={this.state.api_secret || ''}
                buttonAfter={
                  reset.api_secret ? (
                    <Button
                      type="button"
                      onClick={() => {
                        this.undoResetSecret('api_secret');
                      }}>
                      {t('http.undo_reset')}
                    </Button>
                  ) : undefined
                }
              />
            )}
            <Checkbox
              id="api_key_as_header"
              name="api_key_as_header"
              onChange={this.handleChange}
              checked={config.api_key_as_header}>
              {t('http.api_key_as_header')}
            </Checkbox>
          </Col>
        </Row>
      </>
    );
  }
}

export default HttpNotificationForm;
