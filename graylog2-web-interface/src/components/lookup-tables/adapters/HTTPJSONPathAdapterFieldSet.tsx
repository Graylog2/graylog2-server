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
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Input } from 'components/bootstrap';
import { URLAllowListInput, KeyValueTable } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

type Headers = { [key: string]: string };

type Config = {
  headers: Headers;
  url: string;
  single_value_jsonpath: string;
  multi_value_jsonpath: string;
  user_agent: string;
};

type Props = {
  config: Config;
  updateConfig: (config: Config) => void;
  handleFormEvent: (event: SyntheticEvent<EventTarget>) => void;
  validationState: (state: string) => 'error' | 'warning' | 'success';
  validationMessage: (field: string, message: string) => string;
} & WithTranslation;

class HTTPJSONPathAdapterFieldSet extends React.Component<Props> {
  onHTTPHeaderUpdate = (headers: Headers) => {
    const { config, updateConfig } = this.props;
    const configChange = ObjectUtils.clone(config);

    configChange.headers = headers;
    updateConfig(configChange);
  };

  render() {
    const { config, handleFormEvent, validationMessage, validationState, t } = this.props;

    return (
      <fieldset>
        <URLAllowListInput
          label={t('http_doc.lookup_url_label')}
          onChange={handleFormEvent}
          validationMessage={validationMessage('url', t('http_fields.url_validation'))}
          validationState={validationState('url')}
          url={config.url}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
          urlType="regex"
        />
        <Input
          type="text"
          id="single_value_jsonpath"
          name="single_value_jsonpath"
          label={t('http_doc.single_value_label')}
          required
          onChange={handleFormEvent}
          help={validationMessage('single_value_jsonpath', t('http_fields.single_value_help'))}
          bsStyle={validationState('single_value_jsonpath')}
          value={config.single_value_jsonpath}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
        <Input
          type="text"
          id="multi_value_jsonpath"
          name="multi_value_jsonpath"
          label={t('http_doc.multi_value_label')}
          onChange={handleFormEvent}
          help={validationMessage('multi_value_jsonpath', t('http_fields.multi_value_help'))}
          bsStyle={validationState('multi_value_jsonpath')}
          value={config.multi_value_jsonpath}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
        <Input
          type="text"
          id="user_agent"
          name="user_agent"
          label={t('http_doc.http_useragent_label')}
          required
          onChange={handleFormEvent}
          help={t('http_fields.user_agent_help')}
          value={config.user_agent}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9"
        />
        <Input
          id="http_headers"
          label={t('http_fields.headers_label')}
          help={t('http_fields.headers_help')}
          labelClassName="col-sm-3"
          wrapperClassName="col-sm-9">
          <KeyValueTable pairs={config.headers || {}} editable onChange={this.onHTTPHeaderUpdate} />
        </Input>
      </fieldset>
    );
  }
}

export default withTranslation('lookup_tables')(HTTPJSONPathAdapterFieldSet);
