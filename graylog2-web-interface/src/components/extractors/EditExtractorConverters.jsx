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

import { Row, Col, Panel, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import {
  CSVConverterConfiguration,
  DateConverterConfiguration,
  FlexdateConverterConfiguration,
  HashConverterConfiguration,
  IpAnonymizerConverterConfiguration,
  LowercaseConverterConfiguration,
  NumericConverterConfiguration,
  SplitAndCountConverterConfiguration,
  SyslogPriFacilityConverterConfiguration,
  SyslogPriLevelConverterConfiguration,
  TokenizerConverterConfiguration,
  UppercaseConverterConfiguration,
  LookupTableConverterConfiguration,
} from 'components/extractors/converters_configuration';
import ExtractorUtils from 'util/ExtractorUtils';

class EditExtractorConverters extends React.Component {
  static propTypes = {
    extractorType: PropTypes.string.isRequired,
    converters: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      displayedConverters: props.converters.map((converter) => converter.type),
      disabledConverters: {}, // Keep disabled converters configuration, so the user doesn't need to type it again
      selectedConverter: undefined,
    };
  }

  _onConverterSelect = (newValue) => {
    this.setState({ selectedConverter: newValue });
  };

  _onConverterAdd = () => {
    const { displayedConverters, selectedConverter } = this.state;
    const nextDisplayedConverters = displayedConverters.concat(selectedConverter);

    this.setState({ selectedConverter: undefined, displayedConverters: nextDisplayedConverters });
  };

  _onConverterChange = (converterType, converter) => {
    const { disabledConverters } = this.state;
    const { onChange } = this.props;

    if (converter) {
      const newDisabledConverters = disabledConverters;

      if ('converterType' in newDisabledConverters) {
        delete newDisabledConverters[converterType];
        this.setState({ disabledConverters: newDisabledConverters });
      }
    } else {
      const newDisabledConverters = disabledConverters;

      newDisabledConverters[converterType] = this._getConverterByType(converterType);
      this.setState({ disabledConverters: newDisabledConverters });
    }

    onChange(converterType, converter);
  };

  _getConverterOptions = () => {
    const { displayedConverters } = this.state;

    const converterOptions = [];

    Object.keys(ExtractorUtils.ConverterTypes).forEach((converterType) => {
      const type = ExtractorUtils.ConverterTypes[converterType];
      const disabled = displayedConverters.indexOf(type) !== -1;

      converterOptions.push({
        value: type,
        label: ExtractorUtils.getReadableConverterTypeName(type),
        disabled: disabled,
      });
    });

    return converterOptions;
  };

  _getConverterByType = (converterType) => {
    const { converters } = this.props;
    const currentConverter = converters.filter((converter) => converter.type === converterType)[0];

    return (currentConverter ? currentConverter.config : {});
  };

  _getConvertersConfiguration = () => {
    const { displayedConverters, disabledConverters } = this.state;
    const controls = displayedConverters.map((converterType) => {
      // Get converter configuration from disabledConverters if it was disabled
      let converterConfig = this._getConverterByType(converterType);

      if (Object.keys(converterConfig).length === 0 && ('converterType' in disabledConverters)) {
        converterConfig = disabledConverters[converterType];
      }

      switch (converterType) {
        case ExtractorUtils.ConverterTypes.NUMERIC:
          return (
            <NumericConverterConfiguration key={converterType}
                                           type={converterType}
                                           configuration={converterConfig}
                                           onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.DATE:
          return (
            <DateConverterConfiguration key={converterType}
                                        type={converterType}
                                        configuration={converterConfig}
                                        onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.HASH:
          return (
            <HashConverterConfiguration key={converterType}
                                        type={converterType}
                                        configuration={converterConfig}
                                        onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.SPLIT_AND_COUNT:
          return (
            <SplitAndCountConverterConfiguration key={converterType}
                                                 type={converterType}
                                                 configuration={converterConfig}
                                                 onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.IP_ANONYMIZER:
          return (
            <IpAnonymizerConverterConfiguration key={converterType}
                                                type={converterType}
                                                configuration={converterConfig}
                                                onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.SYSLOG_PRI_LEVEL:
          return (
            <SyslogPriLevelConverterConfiguration key={converterType}
                                                  type={converterType}
                                                  configuration={converterConfig}
                                                  onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.SYSLOG_PRI_FACILITY:
          return (
            <SyslogPriFacilityConverterConfiguration key={converterType}
                                                     type={converterType}
                                                     configuration={converterConfig}
                                                     onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.TOKENIZER:
          return (
            <TokenizerConverterConfiguration key={converterType}
                                             type={converterType}
                                             configuration={converterConfig}
                                             onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.CSV:
          return (
            <CSVConverterConfiguration key={converterType}
                                       type={converterType}
                                       configuration={converterConfig}
                                       onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.LOWERCASE:
          return (
            <LowercaseConverterConfiguration key={converterType}
                                             type={converterType}
                                             configuration={converterConfig}
                                             onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.UPPERCASE:
          return (
            <UppercaseConverterConfiguration key={converterType}
                                             type={converterType}
                                             configuration={converterConfig}
                                             onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.FLEXDATE:
          return (
            <FlexdateConverterConfiguration key={converterType}
                                            type={converterType}
                                            configuration={converterConfig}
                                            onChange={this._onConverterChange} />
          );
        case ExtractorUtils.ConverterTypes.LOOKUP_TABLE:
          return (
            <LookupTableConverterConfiguration key={converterType}
                                               type={converterType}
                                               configuration={converterConfig}
                                               onChange={this._onConverterChange} />
          );
        default:
          // eslint-disable-next-line no-console
          console.warn(`Converter type ${converterType} is not supported.`);

          return <></>;
      }
    });

    return controls;
  };

  render() {
    const { extractorType } = this.props;
    const { selectedConverter } = this.state;

    if (extractorType === ExtractorUtils.ExtractorTypes.GROK
      || extractorType === ExtractorUtils.ExtractorTypes.JSON) {
      return (
        <div className="form-group">
          <div className="col-md-offset-2 col-md-10">
            <Panel bsStyle="info" style={{ marginBottom: 0 }}>
              Cannot add converters to{' '}
              <em>{ExtractorUtils.getReadableExtractorTypeName(extractorType)}</em> extractors.
            </Panel>
          </div>
        </div>
      );
    }

    return (
      <div>
        <Input id="add-converter"
               label="Add converter"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help="Add converters to transform the extracted value.">
          <Row className="row-sm">
            <Col md={11}>
              <Select id="add-converter"
                      placeholder="Select a converter"
                      options={this._getConverterOptions()}
                      value={selectedConverter}
                      onChange={this._onConverterSelect} />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onConverterAdd} disabled={!selectedConverter}>
                Add
              </Button>
            </Col>
          </Row>
        </Input>

        {this._getConvertersConfiguration()}
      </div>
    );
  }
}

export default EditExtractorConverters;
