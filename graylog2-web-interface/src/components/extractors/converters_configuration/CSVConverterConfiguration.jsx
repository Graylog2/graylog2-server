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
import FormUtils from 'util/FormsUtils';

class CSVConverterConfiguration extends React.Component {
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
    return (event) => {
      const newConfig = this.props.configuration;

      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(this.props.type, this._getConverterObject(newConfig));
    };
  };

  render() {
    const separatorHelpMessage = (
      <span>
        For example <code>,</code>, <code>\n</code>, and <code>\t</code> will be translated to a single character.
      </span>
    );

    return (
      <div className="xtrc-converter">
        <Input type="checkbox"
               ref={(converterEnabled) => { this.converterEnabled = converterEnabled; }}
               id={`enable-${this.props.type}-converter`}
               label="Add CSV columns as fields"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked
               onChange={this._toggleConverter} />

        <Row className="row-sm">
          <Col md={9} mdOffset={2}>
            <div className="xtrc-converter-subfields">
              <Input type="text"
                     id={`${this.props.type}_converter_column_header`}
                     label="Field names"
                     defaultValue={this.props.configuration.column_header}
                     labelClassName="col-md-3"
                     wrapperClassName="col-md-9"
                     placeholder="field1,field2,field3"
                     onChange={this._onChange('column_header')}
                     required={this.converterEnabled && this.converterEnabled.getChecked()} />

              <Input type="text"
                     id={`${this.props.type}_converter_separator`}
                     label="Separator character"
                     defaultValue={this.props.configuration.separator || ','}
                     labelClassName="col-md-3"
                     wrapperClassName="col-md-9"
                     maxLength="2"
                     onChange={this._onChange('separator')}
                     help={separatorHelpMessage} />

              <Input type="text"
                     id={`${this.props.type}_converter_quote_char`}
                     label="Quote character"
                     defaultValue={this.props.configuration.quote_char || '"'}
                     labelClassName="col-md-3"
                     wrapperClassName="col-md-9"
                     maxLength="1"
                     onChange={this._onChange('quote_char')} />

              <Input type="text"
                     id={`${this.props.type}_converter_escape_char`}
                     label="Escape character"
                     defaultValue={this.props.configuration.escape_char || '\\'}
                     labelClassName="col-md-3"
                     wrapperClassName="col-md-9"
                     maxLength="1"
                     onChange={this._onChange('escape_char')}
                     help="Character used to escape the separator and quote characters." />

              <Input type="checkbox"
                     id={`${this.props.type}_converter_strict_quotes`}
                     label="Use strict quotes"
                     wrapperClassName="col-md-offset-3 col-md-9"
                     defaultChecked={this.props.configuration.strict_quotes}
                     onChange={this._onChange('strict_quotes')}
                     help="Ignore content outside of quotes." />

              <Input type="checkbox"
                     id={`${this.props.type}_converter_trim_leading_whitespace`}
                     label="Trim leading whitespace"
                     wrapperClassName="col-md-offset-3 col-md-9"
                     defaultChecked={this.props.configuration.trim_leading_whitespace}
                     onChange={this._onChange('trim_leading_whitespace')} />
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default CSVConverterConfiguration;
