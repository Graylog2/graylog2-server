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
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Spinner } from 'components/common';
import { Alert, BootstrapModalForm, Input } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

type RuleMetricsConfigProps = {
  config?: any;
  onChange: (...args: any[]) => Promise<unknown>;
  onClose?: (...args: any[]) => void;
} & WithTranslation;

class RuleMetricsConfig extends React.Component<
  RuleMetricsConfigProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    config: undefined,
    onClose: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      nextConfig: props.config,
    };
  }

  saveConfiguration = () => {
    const { onChange } = this.props;
    const { nextConfig } = this.state;

    onChange(nextConfig).then(this.closeModal);
  };

  closeModal = () => {
    this.props.onClose();
  };

  propagateChange = (key, value) => {
    const { config } = this.props;
    const nextConfig = cloneDeep(config);

    nextConfig[key] = value;
    this.setState({ nextConfig });
  };

  handleChange = (event) => {
    const { name } = event.target;

    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  render() {
    const { config, t } = this.props;
    const { nextConfig } = this.state;

    if (!config) {
      return (
        <p>
          <Spinner text={t('metrics_config.loading')} />
        </p>
      );
    }

    return (
      <BootstrapModalForm
        show
        title={t('metrics_config.title')}
        onSubmitForm={this.saveConfiguration}
        onCancel={this.closeModal}
        submitButtonText={t('metrics_config.submit')}>
        <Alert bsStyle="warning">{t('metrics_config.warning')}</Alert>
        <fieldset>
          <Input
            type="radio"
            id="metrics-enabled"
            name="metrics_enabled"
            value="true"
            label={t('metrics_config.enable')}
            onChange={this.handleChange}
            checked={nextConfig.metrics_enabled}
          />

          <Input
            type="radio"
            id="metrics-disabled"
            name="metrics_enabled"
            value="false"
            label={t('metrics_config.disable')}
            onChange={this.handleChange}
            checked={!nextConfig.metrics_enabled}
          />
        </fieldset>
        <p>
          <Trans t={t} i18nKey="metrics_config.pipeline_load_info" components={{ columnName: <strong /> }} />
        </p>
        <p>
          <Trans t={t} i18nKey="metrics_config.rule_load_info" components={{ columnName: <strong /> }} />
        </p>
      </BootstrapModalForm>
    );
  }
}

export default withTranslation('pipelines')(RuleMetricsConfig);
