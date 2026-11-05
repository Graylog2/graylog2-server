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
import { LocaleSelect, TimezoneSelect } from 'components/common';
import { Row, Col, Input } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { getValueFromInput } from 'util/FormsUtils';

// Example value of the format, not translated.
const DATE_FORMAT_EXAMPLE = 'yyyy-MM-dd HH:mm:ss.SSS';

const DocsLink = ({ children = undefined }: { children?: React.ReactNode }) => (
  <DocumentationLink page={DocsHelper.PAGES.PAGE_STANDARD_DATE_CONVERTER} text={children} />
);

type DateConverterConfigurationProps = {
  type: string;
  configuration: any;
  onChange: (...args: any[]) => void;
};

class DateConverterConfiguration extends React.Component<
  DateConverterConfigurationProps,
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

  _onChange = (key) => (data) => {
    const newConfig = this.props.configuration;

    // data can be an event or a value, we need to check its type :sick:
    newConfig[key] = typeof data === 'object' ? getValueFromInput(data.target) : data;
    this.props.onChange(this.props.type, this._getConverterObject(newConfig));
  };

  render() {
    const dateFormatHelpMessage = (
      <span>
        <Trans i18nKey="converters.date_format_help" ns="extractors" components={{ docsLink: <DocsLink /> }} />
      </span>
    );

    const timezoneHelpMessage = (
      <span>
        <Trans i18nKey="converters.time_zone_help" ns="extractors" components={{ docsLink: <DocsLink /> }} />
      </span>
    );

    const localeHelpMessage = (
      <span>
        <Trans i18nKey="converters.locale_help" ns="extractors" components={{ docsLink: <DocsLink /> }} />
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
          label={i18n.t('converters.date_enable', { ns: 'extractors' })}
          wrapperClassName="col-md-offset-2 col-md-10"
          defaultChecked
          onChange={this._toggleConverter}
        />
        <Row className="row-sm">
          <Col md={9} mdOffset={2}>
            <div className="xtrc-converter-subfields">
              <Input
                type="text"
                id={`${this.props.type}_converter_date_format`}
                label={i18n.t('converters.date_format', { ns: 'extractors' })}
                defaultValue={this.props.configuration.date_format}
                labelClassName="col-md-3"
                wrapperClassName="col-md-9"
                placeholder={DATE_FORMAT_EXAMPLE}
                onChange={this._onChange('date_format')}
                required={this.converterEnabled && this.converterEnabled.getChecked()}
                help={dateFormatHelpMessage}
              />

              <Input
                label={i18n.t('converters.time_zone', { ns: 'extractors' })}
                id={`${this.props.type}_converter_timezone`}
                labelClassName="col-sm-3"
                wrapperClassName="col-sm-9"
                help={timezoneHelpMessage}>
                <TimezoneSelect
                  id={`${this.props.type}_converter_timezone`}
                  className="timezone-select"
                  value={this.props.configuration.time_zone}
                  onChange={this._onChange('time_zone')}
                />
              </Input>
              <Input
                label={i18n.t('converters.locale', { ns: 'extractors' })}
                id={`${this.props.type}_converter_locale`}
                labelClassName="col-sm-3"
                wrapperClassName="col-sm-9"
                help={localeHelpMessage}>
                <LocaleSelect
                  id={`${this.props.type}_converter_locale`}
                  className="locale-select"
                  value={this.props.configuration.locale}
                  onChange={this._onChange('locale')}
                />
              </Input>
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default DateConverterConfiguration;
