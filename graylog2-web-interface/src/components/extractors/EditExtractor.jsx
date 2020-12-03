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

import { Col, ControlLabel, FormControl, FormGroup, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import ExtractorUtils from 'util/ExtractorUtils';
import FormUtils from 'util/FormsUtils';

import EditExtractorConverters from './EditExtractorConverters';
import EditExtractorConfiguration from './EditExtractorConfiguration';
import ExtractorExampleMessage from './ExtractorExampleMessage';

const ExtractorsActions = ActionsProvider.getActions('Extractors');
const ToolsStore = StoreProvider.getStore('Tools');

class EditExtractor extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']).isRequired,
    extractor: PropTypes.object.isRequired,
    inputId: PropTypes.string.isRequired,
    exampleMessage: PropTypes.string,
    onSave: PropTypes.func.isRequired,
  };

  static defaultProps = {
    exampleMessage: undefined,
  }

  constructor(props) {
    super(props);

    this.state = {
      updatedExtractor: props.extractor,
      conditionTestResult: undefined,
      exampleMessage: props.exampleMessage,
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    const { exampleMessage } = this.props;

    if (exampleMessage !== nextProps.exampleMessage) {
      this._updateExampleMessage(nextProps.exampleMessage);
    }
  }

  _updateExampleMessage = (nextExample) => {
    this.setState({ exampleMessage: nextExample });
  };

  // Ensures the target field only contains alphanumeric characters and underscores
  _onTargetFieldChange = (event) => {
    const { value } = event.target;
    const newValue = value.replace(/[^\w\d_]/g, '');

    if (value !== newValue) {
      this.targetField.getInputDOMNode().value = newValue;
    }

    this._onFieldChange('target_field')(event);
  };

  _onFieldChange = (key) => {
    return (event) => {
      const nextState = {};
      const { updatedExtractor } = this.state;

      updatedExtractor[key] = FormUtils.getValueFromInput(event.target);
      nextState.updatedExtractor = updatedExtractor;

      // Reset result of testing condition after a change in the input
      if (key === 'condition_value') {
        nextState.conditionTestResult = undefined;
      }

      this.setState(nextState);
    };
  };

  _onConfigurationChange = (newConfiguration) => {
    const { updatedExtractor } = this.state;

    updatedExtractor.extractor_config = newConfiguration;
    this.setState({ updatedExtractor: updatedExtractor });
  };

  _onConverterChange = (converterType, newConverter) => {
    const { updatedExtractor } = this.state;
    const previousConverter = updatedExtractor.converters.filter((converter) => converter.type === converterType)[0];

    if (previousConverter) {
      // Remove converter from the list
      const position = updatedExtractor.converters.indexOf(previousConverter);

      updatedExtractor.converters.splice(position, 1);
    }

    if (newConverter) {
      updatedExtractor.converters.push(newConverter);
    }

    this.setState({ updatedExtractor: updatedExtractor });
  };

  _testCondition = () => {
    const { exampleMessage, updatedExtractor } = this.state;
    const tester = (updatedExtractor.condition_type === 'string' ? ToolsStore.testContainsString : ToolsStore.testRegex);
    const promise = tester(updatedExtractor.condition_value, exampleMessage);

    promise.then((result) => this.setState({ conditionTestResult: result.matched }));
  };

  _tryButtonDisabled = () => {
    const { updatedExtractor, exampleMessage } = this.state;

    return (updatedExtractor.condition_value === ''
      || updatedExtractor.condition_value === undefined
      || !exampleMessage);
  };

  _getExtractorConditionControls = () => {
    const { conditionTestResult, updatedExtractor } = this.state;

    if (!updatedExtractor.condition_type
      || updatedExtractor.condition_type === 'none') {
      return <div />;
    }

    let conditionInputLabel;
    let conditionInputHelp;

    if (updatedExtractor.condition_type === 'string') {
      conditionInputLabel = 'Field contains string';
      conditionInputHelp = 'Type a string that the field should contain in order to attempt the extraction.';
    } else {
      conditionInputLabel = 'Field matches regular expression';
      conditionInputHelp = 'Type a regular expression that the field should contain in order to attempt the extraction.';
    }

    let inputStyle;

    if (conditionTestResult === true) {
      inputStyle = 'success';
      conditionInputHelp = 'Matches! Extractor would run against this example.';
    } else if (conditionTestResult === false) {
      inputStyle = 'error';
      conditionInputHelp = 'Does not match! Extractor would not run.';
    }

    return (
      <div>
        <Input id="condition_value"
               label={conditionInputLabel}
               bsStyle={inputStyle}
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help={conditionInputHelp}>
          <Row className="row-sm">
            <Col md={11}>
              <input type="text"
                     id="condition_value"
                     className="form-control"
                     defaultValue={updatedExtractor.condition_value}
                     onChange={this._onFieldChange('condition_value')}
                     required />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info"
                      onClick={this._testCondition}
                      disabled={this._tryButtonDisabled()}>
                Try
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  };

  _saveExtractor = (event) => {
    const { inputId, onSave } = this.props;
    const { updatedExtractor } = this.state;

    event.preventDefault();

    ExtractorsActions.save.triggerPromise(inputId, updatedExtractor)
      .then(() => onSave());
  };

  _staticField = (label, text) => {
    return (
      <FormGroup>
        <Col componentClass={ControlLabel} md={2}>
          {label}
        </Col>
        <Col md={10}>
          <FormControl.Static>{text}</FormControl.Static>
        </Col>
      </FormGroup>
    );
  };

  render() {
    const { updatedExtractor, exampleMessage } = this.state;
    const { action } = this.props;
    const conditionTypeHelpMessage = 'Extracting only from messages that match a certain condition helps you '
      + 'avoiding wrong or unnecessary extractions and can also save CPU resources.';

    const cursorStrategyHelpMessage = (
      <span>
        Do you want to copy or cut from source? You cannot use the cutting feature on standard fields like{' '}
        <em>message</em> and <em>source</em>.
      </span>
    );

    const targetFieldHelpMessage = (
      <span>
        Choose a field name to store the extracted value. It can only contain <b>alphanumeric characters and underscores</b>. Example: <em>http_response_code</em>.
      </span>
    );

    let storeAsFieldInput;

    // Grok and JSON extractors create their required fields, so no need to add an input for them
    if (updatedExtractor.type !== ExtractorUtils.ExtractorTypes.GROK && updatedExtractor.type !== ExtractorUtils.ExtractorTypes.JSON) {
      storeAsFieldInput = (
        <Input type="text"
               ref={(targetField) => { this.targetField = targetField; }}
               id="target_field"
               label="Store as field"
               defaultValue={updatedExtractor.target_field}
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               onChange={this._onTargetFieldChange}
               required
               help={targetFieldHelpMessage} />
      );
    }

    return (
      <div>
        <Row className="content extractor-list">
          <Col md={12}>
            <h2>Example message</h2>
            <Row style={{ marginTop: 5 }}>
              <Col md={12}>
                <ExtractorExampleMessage field={updatedExtractor.source_field}
                                         example={exampleMessage}
                                         onExampleLoad={this._updateExampleMessage} />
              </Col>
            </Row>
            <h2>Extractor configuration</h2>
            <Row>
              <Col md={8}>
                <form className="extractor-form form-horizontal" method="POST" onSubmit={this._saveExtractor}>
                  {this._staticField('Extractor type', ExtractorUtils.getReadableExtractorTypeName(updatedExtractor.type))}
                  {this._staticField('Source field', updatedExtractor.source_field)}

                  <EditExtractorConfiguration extractorType={updatedExtractor.type}
                                              configuration={updatedExtractor.extractor_config}
                                              onChange={this._onConfigurationChange}
                                              exampleMessage={exampleMessage} />

                  <Input id="condition-type"
                         label="Condition"
                         labelClassName="col-md-2"
                         wrapperClassName="col-md-10"
                         help={conditionTypeHelpMessage}>
                    <span>
                      <div className="radio">
                        <label htmlFor="condition_type_none">
                          <input type="radio"
                                 name="condition_type"
                                 id="condition_type_none"
                                 value="none"
                                 onChange={this._onFieldChange('condition_type')}
                                 defaultChecked={!updatedExtractor.condition_type || updatedExtractor.condition_type === 'none'} />
                          Always try to extract
                        </label>
                      </div>
                      <div className="radio">
                        <label htmlFor="condition_type_string">
                          <input type="radio"
                                 name="condition_type"
                                 id="condition_type_string"
                                 value="string"
                                 onChange={this._onFieldChange('condition_type')}
                                 defaultChecked={updatedExtractor.condition_type === 'string'} />
                          Only attempt extraction if field contains string
                        </label>
                      </div>
                      <div className="radio">
                        <label htmlFor="condition_type_regex">
                          <input type="radio"
                                 name="condition_type"
                                 id="condition_type_regex"
                                 value="regex"
                                 onChange={this._onFieldChange('condition_type')}
                                 defaultChecked={updatedExtractor.condition_type === 'regex'} />
                          Only attempt extraction if field matches regular expression
                        </label>
                      </div>
                    </span>
                  </Input>
                  {this._getExtractorConditionControls()}

                  {storeAsFieldInput}

                  <Input id="extraction-strategy"
                         label="Extraction strategy"
                         labelClassName="col-md-2"
                         wrapperClassName="col-md-10"
                         help={cursorStrategyHelpMessage}>
                    <span>
                      <label className="radio-inline" htmlFor="cursor_strategy_copy">
                        <input type="radio"
                               name="cursor_strategy"
                               id="cursor_strategy_copy"
                               value="copy"
                               onChange={this._onFieldChange('cursor_strategy')}
                               defaultChecked={!updatedExtractor.cursor_strategy || updatedExtractor.cursor_strategy === 'copy'} />
                        Copy
                      </label>
                      <label className="radio-inline" htmlFor="cursor_strategy_cut">
                        <input type="radio"
                               name="cursor_strategy"
                               id="cursor_strategy_cut"
                               value="cut"
                               onChange={this._onFieldChange('cursor_strategy')}
                               defaultChecked={updatedExtractor.cursor_strategy === 'cut'} />
                        Cut
                      </label>
                    </span>
                  </Input>

                  <Input type="text"
                         id="title"
                         label="Extractor title"
                         defaultValue={updatedExtractor.title}
                         labelClassName="col-md-2"
                         wrapperClassName="col-md-10"
                         onChange={this._onFieldChange('title')}
                         required
                         help="A descriptive name for this extractor." />

                  <div style={{ marginBottom: 20 }}>
                    <EditExtractorConverters extractorType={updatedExtractor.type}
                                             converters={updatedExtractor.converters}
                                             onChange={this._onConverterChange} />
                  </div>

                  <Row>
                    <Col mdOffset={2} md={10}>
                      <Button type="submit" bsStyle="success">
                        {action === 'create' ? 'Create extractor' : 'Update extractor'}
                      </Button>
                    </Col>
                  </Row>
                </form>
              </Col>
            </Row>
          </Col>
        </Row>
      </div>
    );
  }
}

export default EditExtractor;
