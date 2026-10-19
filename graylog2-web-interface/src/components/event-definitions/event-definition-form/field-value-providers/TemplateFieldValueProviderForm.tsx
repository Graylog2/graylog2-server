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
import type { FieldValueProvider } from 'graylog-web-plugin';
import { Trans } from 'react-i18next';

import i18n from 'i18n';

import { ExternalLink } from 'components/common';
import { Checkbox, Col, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

import TemplateFieldValueProviderPreview from './TemplateFieldValueProviderPreview';

const t = (key: string) => i18n.t(key, { ns: 'event_definitions' });

const JmteLink = ({ children = undefined }: { children?: React.ReactNode }) => (
  <ExternalLink href="https://cdn.rawgit.com/DJCordhose/jmte/master/doc/index.html">{children}</ExternalLink>
);

type TemplateFieldValueProviderFormProps = React.ComponentProps<FieldValueProvider['formComponent']>;

class TemplateFieldValueProviderForm extends React.Component<
  TemplateFieldValueProviderFormProps,
  {
    [key: string]: any;
  }
> {
  static type = 'template-v1';

  static defaultConfig = {
    template: '',
    exclude_empty_fields: true,
  };

  static requiredFields = ['template'];

  handleChange = (event) => {
    const { config, onChange } = this.props;
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const nextProviders = cloneDeep(config.providers);
    const templateProvider = nextProviders.find((provider) => provider.type === TemplateFieldValueProviderForm.type);

    templateProvider[name] = value;

    // Requiring all values turns an unset variable into an error, so an empty value cannot occur.
    if (name === 'require_values' && value) {
      templateProvider.exclude_empty_fields = true;
    }

    onChange({ ...config, providers: nextProviders });
  };

  render() {
    const { config, validation } = this.props;
    const provider = config.providers.find((p) => p.type === TemplateFieldValueProviderForm.type);

    const helpText = (
      <span>
        <Trans
          i18n={i18n}
          ns="event_definitions"
          i18nKey="template_provider_form.template_help"
          components={{ jmteLink: <JmteLink /> }}
        />
      </span>
    );

    return (
      <Row className="row-sm">
        <Col md={7} lg={6}>
          <Input
            id="template-provider-template"
            name="template"
            type="text"
            label={t('template_provider_form.template')}
            onChange={this.handleChange}
            value={provider.template || ''}
            bsStyle={validation.errors.template ? 'error' : null}
            help={validation.errors.template || helpText}
          />

          <FormGroup>
            <Checkbox
              id="lookup-message-require-values"
              name="require_values"
              checked={provider.require_values}
              onChange={this.handleChange}>
              {t('template_provider_form.require_values')}
            </Checkbox>
            <HelpBlock>{t('template_provider_form.require_values_help')}</HelpBlock>
          </FormGroup>

          <FormGroup>
            <Checkbox
              id="template-provider-exclude-empty-fields"
              name="exclude_empty_fields"
              disabled={provider.require_values}
              checked={provider.exclude_empty_fields ?? true}
              onChange={this.handleChange}>
              {t('template_provider_form.exclude_empty_fields')}
            </Checkbox>
            <HelpBlock>{t('template_provider_form.exclude_empty_fields_help')}</HelpBlock>
          </FormGroup>
        </Col>
        <Col md={5} lgOffset={1}>
          <TemplateFieldValueProviderPreview />
        </Col>
      </Row>
    );
  }
}

export default TemplateFieldValueProviderForm;
