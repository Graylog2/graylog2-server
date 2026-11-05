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
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Icon } from 'components/common';
import { Col, Row, Button, Input } from 'components/bootstrap';
import ExtractorUtils from 'util/ExtractorUtils';
import { getValueFromInput } from 'util/FormsUtils';
import { testJSON } from 'api/tools';

type Configuration = {
  flatten: boolean;
  list_separator: string;
  key_separator: string;
  kv_separator: string;
  replace_key_whitespace: boolean;
  key_whitespace_replacement: string;
  key_prefix: string;
  string: string;
};
type Props = {
  configuration: Configuration;
  exampleMessage?: string;
  onChange: (newConfig: Configuration) => void;
  onExtractorPreviewLoad: (content: React.ReactNode) => void;
} & WithTranslation;
type State = {
  configuration: Configuration;
  trying: boolean;
};

class JSONExtractorConfiguration extends React.Component<Props, State> {
  static defaultProps = {
    exampleMessage: undefined,
  };

  constructor(props: Props, context: any) {
    super(props, context);

    this.state = {
      trying: false,
      configuration: this._getEffectiveConfiguration(props.configuration),
    };
  }

  componentDidMount() {
    this.props.onChange(this.state.configuration);
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ configuration: this._getEffectiveConfiguration(nextProps.configuration) });
  }

  private DEFAULT_CONFIGURATION = {
    list_separator: ', ',
    key_separator: '_',
    kv_separator: '=',
    key_prefix: '',
    replace_key_whitespace: false,
    key_whitespace_replacement: '_',
  };

  _onTryClick = () => {
    this.setState({ trying: true });

    const { configuration } = this.state;
    const promise = testJSON(
      configuration.flatten,
      configuration.list_separator,
      configuration.key_separator,
      configuration.kv_separator,
      configuration.replace_key_whitespace,
      configuration.key_whitespace_replacement,
      configuration.key_prefix,
      this.props.exampleMessage,
    );

    promise.then((result) => {
      const matches = [];

      for (const match in result.matches) {
        if (match in result.matches) {
          matches.push(<dt key={`${match}-name`}>{match}</dt>);
          matches.push(
            <dd key={`${match}-value`}>
              <samp>{result.matches[match]}</samp>
            </dd>,
          );
        }
      }

      const preview = matches.length === 0 ? '' : <dl>{matches}</dl>;

      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  };

  _getEffectiveConfiguration(configuration) {
    return ExtractorUtils.getEffectiveConfiguration(this.DEFAULT_CONFIGURATION, configuration) as Configuration;
  }

  _onChange(key) {
    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      const newConfig = this.state.configuration;

      newConfig[key] = getValueFromInput(event.target);
      this.props.onChange(newConfig);
    };
  }

  _isTryButtonDisabled() {
    return this.state.trying || !this.props.exampleMessage;
  }

  render() {
    const { t } = this.props;

    return (
      <div>
        <Input
          type="checkbox"
          id="flatten"
          label={t('json.flatten')}
          wrapperClassName="col-md-offset-2 col-md-10"
          defaultChecked={this.state.configuration.flatten}
          onChange={this._onChange('flatten')}
          help={t('json.flatten_help')}
        />

        <Input
          type="text"
          id="list_separator"
          label={t('json.list_separator')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          defaultValue={this.state.configuration.list_separator}
          required
          onChange={this._onChange('list_separator')}
          help={t('json.list_separator_help')}
        />

        <Input
          type="text"
          id="key_separator"
          label={t('json.key_separator')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          defaultValue={this.state.configuration.key_separator}
          required
          onChange={this._onChange('key_separator')}
          help={
            <span>
              <Trans t={t} i18nKey="json.key_separator_help" components={{ em: <em /> }} />
            </span>
          }
        />

        <Input
          type="text"
          id="kv_separator"
          label={t('json.kv_separator')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          defaultValue={this.state.configuration.kv_separator}
          required
          onChange={this._onChange('kv_separator')}
          help={t('json.kv_separator_help')}
        />

        <Input
          type="text"
          id="key_prefix"
          label={t('json.key_prefix')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          defaultValue={this.state.configuration.key_prefix}
          onChange={this._onChange('key_prefix')}
          help={t('json.key_prefix_help')}
        />

        <Input
          type="checkbox"
          id="replace_key_whitespace"
          label={t('json.replace_key_whitespace')}
          wrapperClassName="col-md-offset-2 col-md-10"
          defaultChecked={this.state.configuration.replace_key_whitespace}
          onChange={this._onChange('replace_key_whitespace')}
          help={t('json.replace_key_whitespace_help')}
        />

        <Input
          type="text"
          id="key_whitespace_replacement"
          label={t('json.key_whitespace_replacement')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          defaultValue={this.state.configuration.key_whitespace_replacement}
          disabled={!this.state.configuration.replace_key_whitespace}
          required
          onChange={this._onChange('key_whitespace_replacement')}
          help={t('json.key_whitespace_replacement_help')}
        />

        <Row>
          <Col mdOffset={2} md={10}>
            <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
              {this.state.trying ? <Icon name="progress_activity" spin /> : t('edit.try')}
            </Button>
          </Col>
        </Row>
      </div>
    );
  }
}

export default withTranslation('extractors')(JSONExtractorConfiguration);
