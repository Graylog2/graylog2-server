import React, {PropTypes} from 'react';
import {Row, Col, Input, Button, Panel} from 'react-bootstrap';
import {Select} from 'components/common';

import {NumericConverterConfiguration} from 'components/extractors/converters_configuration';

import ExtractorUtils from 'util/ExtractorUtils';

const EditExtractorConverters = React.createClass({
  propTypes: {
    extractorType: PropTypes.string.isRequired,
    converters: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      displayedConverters: this.props.converters.map(converter => converter.type),
      selectedConverter: undefined,
    };
  },
  _onConverterSelect(newValue) {
    this.setState({selectedConverter: newValue});
  },
  _onConverterAdd() {
    const newDisplayedConverters = this.state.displayedConverters;
    newDisplayedConverters.push(this.state.selectedConverter);
    this.setState({selectedConverter: undefined, converters: newDisplayedConverters});
  },
  _getConverterOptions() {
    const converterOptions = [];
    Object.keys(ExtractorUtils.ConverterTypes).forEach(converterType => {
      const type = ExtractorUtils.ConverterTypes[converterType];
      const disabled = this.state.displayedConverters.indexOf(type) !== -1;
      converterOptions.push({
        value: type,
        label: ExtractorUtils.getReadableConverterTypeName(type),
        disabled: disabled,
      });
    });

    return converterOptions;
  },
  _getConvertersConfiguration() {
    const controls = this.state.displayedConverters.map(converterType => {
      const converter = this.props.converters.filter(converter => converter.type === converterType)[0];
      const converterConfig = converter ? converter.config : {};

      switch (converterType) {
      case ExtractorUtils.ConverterTypes.NUMERIC:
        return (
          <NumericConverterConfiguration key={converterType}
                                         type={converterType}
                                         configuration={converterConfig}
                                         onChange={this.props.onChange}/>
        );
      default:
        console.warn(`Converter type ${converterType} is not supported.`);
      }
    });

    return controls;
  },
  render() {
    if (this.props.extractorType === ExtractorUtils.ExtractorTypes.GROK || this.props.extractorType === ExtractorUtils.ExtractorTypes.JSON) {
      return (
        <div className="form-group">
          <div className="col-md-offset-2 col-md-10">
            <Panel bsStyle="info" style={{marginBottom: 0}}>
              Cannot add converters to <em>{ExtractorUtils.getReadableExtractorTypeName(this.props.extractorType)}</em>
              extractors.
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
              <Select ref="addConverter"
                      id="add-converter"
                      placeholder="Select a converter"
                      options={this._getConverterOptions()}
                      value={this.state.selectedConverter}
                      onChange={this._onConverterSelect}/>
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onConverterAdd} disabled={!this.state.selectedConverter}>
                Add
              </Button>
            </Col>
          </Row>
        </Input>

        {this._getConvertersConfiguration()}
      </div>
    );
  },
});

export default EditExtractorConverters;
