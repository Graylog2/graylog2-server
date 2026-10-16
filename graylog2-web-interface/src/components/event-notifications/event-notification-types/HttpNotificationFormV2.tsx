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
import styled from 'styled-components';
import { Trans } from 'react-i18next';

import i18n from 'i18n';
import { Select, SourceCodeEditor, TimezoneSelect, URLAllowListInput } from 'components/common';
import { Button, Checkbox, Col, ControlLabel, FormGroup, HelpBlock, Input, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';
import type { EventNotificationTypes } from 'components/event-notifications/types';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';

import { DEFAULT_JSON_TEMPLATE, DEFAULT_FORM_PARAM_TEMPLATE, DEFAULT_PLAIN_TEXT_TEMPLATE } from './templates';

const StyledButton = styled(Button)`
  clear: both;
  display: block;
  margin-bottom: 15px;
`;

type Props = React.ComponentProps<EventNotificationTypes['formComponent']>;

// The form is a class component, so texts are translated at render time.
const t = (key: string) => i18n.t(key, { ns: 'event_notifications' });

const AlertsDocsLink = ({ children = undefined }: { children?: React.ReactNode }) => (
  <DocumentationLink page={DocsHelper.PAGES.ALERTS} text={children} />
);

// Only populate the new template if no changes have been made to the existing body or it is empty
const shouldPopulateTemplate = (currentType: string, currentBody: string): boolean => {
  if (currentBody === '') {
    return true;
  }

  if (currentType === 'JSON') {
    return currentBody === DEFAULT_JSON_TEMPLATE;
  }

  if (currentType === 'FORM_DATA') {
    return currentBody === DEFAULT_FORM_PARAM_TEMPLATE;
  }

  return currentBody === DEFAULT_PLAIN_TEXT_TEMPLATE;
};

class HttpNotificationFormV2 extends React.Component<Props, any> {
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

  static defaultConfig = {
    url: '',
    api_key_as_header: false,
    api_key: '',
    api_secret: { keep_value: true },
    basic_auth: { keep_value: true },
    skip_tls_verification: false,
    method: 'POST',
    time_zone: 'UTC',
    body_template: DEFAULT_JSON_TEMPLATE,
    content_type: 'JSON',
    headers: '',
  };

  propagateChange = (key: string, value: any) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);

    nextConfig[key] = value;
    onChange(nextConfig);
  };

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name } = event.target;
    const inputValue = FormsUtils.getValueFromInput(event.target);

    this.propagateChange(name, inputValue);
  };

  handleUrlChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.propagateChange('url', event.target.value);
  };

  handleTimeZoneChange = (nextValue: string) => {
    this.propagateChange('time_zone', nextValue);
  };

  handleJsonBodyTemplateChange = (nextValue: string) => {
    this.propagateChange('body_template', nextValue);
  };

  handleMethodChange = (nextValue: string) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    nextConfig.method = nextValue;

    if (nextValue === 'GET') {
      nextConfig.body_template = null;
      nextConfig.content_type = null;
    } else if (config.method === 'GET') {
      nextConfig.content_type = 'JSON';
      nextConfig.body_template = '';
    }

    onChange(nextConfig);
  };

  handleContentTypeChange = (nextValue: string) => {
    const { config, onChange } = this.props;
    const nextConfig = cloneDeep(config);
    nextConfig.content_type = nextValue;

    if (shouldPopulateTemplate(config.content_type, config.body_template)) {
      if (nextValue === 'JSON') {
        nextConfig.body_template = DEFAULT_JSON_TEMPLATE;
      } else if (nextValue === 'FORM_DATA') {
        nextConfig.body_template = DEFAULT_FORM_PARAM_TEMPLATE;
      } else {
        nextConfig.body_template = DEFAULT_PLAIN_TEXT_TEMPLATE;
      }
    }

    onChange(nextConfig);
  };

  handleSecretInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name } = event.target;
    const inputValue = String(FormsUtils.getValueFromInput(event.target));
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

    const httpMethods = [
      { value: 'POST', label: 'POST' },
      { value: 'GET', label: 'GET' },
      { value: 'PUT', label: 'PUT' },
    ];
    const contentTypes = [
      { value: 'JSON', label: 'application/json' },
      { value: 'FORM_DATA', label: 'application/x-www-form-urlencoded' },
      { value: 'PLAIN_TEXT', label: 'text/plain' },
    ];
    const helpElement = (
      <p>
        <Trans ns="event_notifications" i18nKey="http_v2.body_help" components={{ docsLink: <AlertsDocsLink /> }} />
      </p>
    );

    return (
      <>
        <URLAllowListInput
          label={t('http.url')}
          onChange={this.handleUrlChange}
          validationState={validation.errors.url ? 'error' : null}
          validationMessage={validation?.errors?.url?.[0] ?? t('http_v2.url_help')}
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
                help={t('http_v2.basic_auth_help')}
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
          <Col md={6}>
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
              help={validation.errors.api_key?.[0] ?? t('http_v2.api_key_help')}
              value={config.api_key}
            />
            <Checkbox
              id="api_key_as_header"
              name="api_key_as_header"
              onChange={this.handleChange}
              checked={config.api_key_as_header}>
              {t('http.api_key_as_header')}
            </Checkbox>
          </Col>
          <Col md={6}>
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
                help={validation.errors.api_secret?.[0] ?? t('http_v2.api_secret_help')}
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
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            <Input
              id="headers"
              name="headers"
              label={
                <span>
                  {t('http_v2.headers')} <small className="text-muted">{t('form.optional')}</small>
                </span>
              }
              type="text"
              onChange={this.handleChange}
              bsStyle={validation.errors.headers ? 'error' : null}
              help={validation.errors.headers?.[0] ?? t('http_v2.headers_help')}
              value={config.headers}
            />
          </Col>
        </Row>
        <Row>
          <Col md={4}>
            <Input help={t('http_v2.method_help')} id="notification-method" label={t('http_v2.method')}>
              <Select
                id="method"
                name="method"
                clearable={false}
                options={httpMethods}
                onChange={this.handleMethodChange}
                value={config.method}
              />
            </Input>
          </Col>
          <Col md={4}>
            <Input
              help={t('http_v2.content_type_help')}
              id="notification-content-type"
              label={t('http_v2.content_type')}>
              <Select
                id="content-type"
                name="content-type"
                options={contentTypes}
                disabled={config.method === 'GET'}
                onChange={this.handleContentTypeChange}
                clearable={false}
                value={config.content_type}
              />
            </Input>
          </Col>
          <Col md={4}>
            <Input id="notification-time-zone" help={t('http_v2.time_zone_help')} label={t('http_v2.time_zone')}>
              <TimezoneSelect
                className="timezone-select"
                name="time_zone"
                disabled={config.method === 'GET'}
                value={config.time_zone}
                clearable={false}
                onChange={this.handleTimeZoneChange}
              />
            </Input>
          </Col>
        </Row>
        <Row>
          <Col md={12}>
            {config.method !== 'GET' && (
              <FormGroup
                controlId="notification-body-template"
                validationState={validation.errors.body_template ? 'error' : null}>
                <ControlLabel>{t('http_v2.body_template')}</ControlLabel>
                <SourceCodeEditor
                  id="notification-body-template"
                  mode="text"
                  theme="light"
                  value={config.body_template || ''}
                  wrapEnabled
                  onChange={this.handleJsonBodyTemplateChange}
                />
                <HelpBlock>{validation.errors.body_template?.[0] ?? helpElement}</HelpBlock>
              </FormGroup>
            )}
          </Col>
        </Row>
      </>
    );
  }
}

export default HttpNotificationFormV2;
