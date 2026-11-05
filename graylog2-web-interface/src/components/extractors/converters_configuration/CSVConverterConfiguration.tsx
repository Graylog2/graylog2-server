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
import { Trans } from 'react-i18next';

import i18n from 'i18n';
import { Row, Col, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';

// Example values of the format, not translated.
const FIELD_NAMES_EXAMPLE = 'field1,field2,field3';

type CSVConverterConfigurationProps = {
  type: string;
  configuration: any;
  onChange: (...args: any[]) => void;
};

class CSVConverterConfiguration extends React.Component<
  CSVConverterConfigurationProps,
  {
    [key: string]: any;
  }
> {
  componentDidMount() {
    this.props.onChange(this.props.type, this._getConverterObject());
  }

  private converterEnabled: Input;

  _getConverterObject = (configuration?) => ({
    type: this.props.type,
    config: configuration || this.props.configuration,
  });

  _toggleConverter = (event) => {
    let converter;

    if (getValueFromInput(event.target) === true) {
      converter = this._getConverterObject();
    }

    this.props.onChange(this.props.type, converter);
  };

  _onChange = (key) => (event) => {
    const newConfig = this.props.configuration;

    newConfig[key] = getValueFromInput(event.target);
    this.props.onChange(this.props.type, this._getConverterObject(newConfig));
  };

  render() {
    const separatorHelpMessage = (
      <span>
        <Trans i18nKey="converters.csv_separator_help" ns="extractors" components={{ code: <code /> }} />
      </span>
    );

    return (
      <div className="xtrc-converter">
        <Input
          type="checkbox"
          ref={(converterEnabled) => {
            this.converterEnabled = converterEnabled;
          }}
          id={`enable-${this.props.type}-converter`}
          label={i18n.t('converters.csv_enable', { ns: 'extractors' })}
          wrapperClassName="col-md-offset-2 col-md-10"
          defaultChecked
          onChange={this._toggleConverter}
        />

        <Row className="row-sm">
          <Col md={9} mdOffset={2}>
            <div className="xtrc-converter-subfields">
              <Input
                type="text"
                id={`${this.props.type}_converter_column_header`}
                label={i18n.t('converters.csv_field_names', { ns: 'extractors' })}
                defaultValue={this.props.configuration.column_header}
                labelClassName="col-md-3"
                wrapperClassName="col-md-9"
                placeholder={FIELD_NAMES_EXAMPLE}
                onChange={this._onChange('column_header')}
                required={this.converterEnabled && this.converterEnabled.getChecked()}
              />

              <Input
                type="text"
                id={`${this.props.type}_converter_separator`}
                label={i18n.t('converters.csv_separator', { ns: 'extractors' })}
                defaultValue={this.props.configuration.separator || ','}
                labelClassName="col-md-3"
                wrapperClassName="col-md-9"
                maxLength={2}
                onChange={this._onChange('separator')}
                help={separatorHelpMessage}
              />

              <Input
                type="text"
                id={`${this.props.type}_converter_quote_char`}
                label={i18n.t('converters.csv_quote', { ns: 'extractors' })}
                defaultValue={this.props.configuration.quote_char || '"'}
                labelClassName="col-md-3"
                wrapperClassName="col-md-9"
                maxLength={1}
                onChange={this._onChange('quote_char')}
              />

              <Input
                type="text"
                id={`${this.props.type}_converter_escape_char`}
                label={i18n.t('converters.csv_escape', { ns: 'extractors' })}
                defaultValue={this.props.configuration.escape_char || '\\'}
                labelClassName="col-md-3"
                wrapperClassName="col-md-9"
                maxLength={1}
                onChange={this._onChange('escape_char')}
                help={i18n.t('converters.csv_escape_help', { ns: 'extractors' })}
              />

              <Input
                type="checkbox"
                id={`${this.props.type}_converter_strict_quotes`}
                label={i18n.t('converters.csv_strict_quotes', { ns: 'extractors' })}
                wrapperClassName="col-md-offset-3 col-md-9"
                defaultChecked={this.props.configuration.strict_quotes}
                onChange={this._onChange('strict_quotes')}
                help={i18n.t('converters.csv_strict_quotes_help', { ns: 'extractors' })}
              />

              <Input
                type="checkbox"
                id={`${this.props.type}_converter_trim_leading_whitespace`}
                label={i18n.t('converters.csv_trim', { ns: 'extractors' })}
                wrapperClassName="col-md-offset-3 col-md-9"
                defaultChecked={this.props.configuration.trim_leading_whitespace}
                onChange={this._onChange('trim_leading_whitespace')}
              />
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default CSVConverterConfiguration;
