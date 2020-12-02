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
import PropTypes from 'prop-types';
import React from 'react';

import { Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { TimezoneSelect } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import FormUtils from 'util/FormsUtils';

class FlexdateConverterConfiguration extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.onChange(this.props.type, this._getConverterObject());
  }

  _getConverterObject = (configuration) => {
    return { type: this.props.type, config: configuration || this.props.configuration };
  };

  _toggleConverter = (event) => {
    let converter;

    if (FormUtils.getValueFromInput(event.target) === true) {
      converter = this._getConverterObject();
    }

    this.props.onChange(this.props.type, converter);
  };

  _onChange = (key) => {
    return (data) => {
      const newConfig = this.props.configuration;

      // data can be an event or a value, we need to check its type :sick:
      newConfig[key] = typeof data === 'object' ? FormUtils.getValueFromInput(data.target) : data;
      this.props.onChange(this.props.type, this._getConverterObject(newConfig));
    };
  };

  render() {
    const timezoneHelpMessage = (
      <span>
        Time zone to apply to date. Read more in the <DocumentationLink page={DocsHelper.PAGES.PAGE_FLEXIBLE_DATE_CONVERTER} text="documentation" />.
      </span>
    );

    return (
      <div className="xtrc-converter">
        <Input type="checkbox"
               id={`enable-${this.props.type}-converter`}
               label="Flexibly parse date"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked
               onChange={this._toggleConverter} />
        <Row className="row-sm">
          <Col md={9} mdOffset={2}>
            <div className="xtrc-converter-subfields">
              <Input label="Time Zone"
                     id={`${this.props.type}_converter_timezone`}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     help={timezoneHelpMessage}>
                <TimezoneSelect id={`${this.props.type}_converter_timezone`}
                                className="timezone-select"
                                value={this.props.configuration.time_zone}
                                onChange={this._onChange('time_zone')} />
              </Input>
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default FlexdateConverterConfiguration;
