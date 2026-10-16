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
import { Col, ControlLabel, FormControl, FormGroup, Row, Button, Input } from 'components/bootstrap';
import ExtractorUtils from 'util/ExtractorUtils';
import { getValueFromInput } from 'util/FormsUtils';
import { testContainsString, testRegex } from 'api/tools';
import { saveExtractor } from 'hooks/useExtractors';

import EditExtractorConverters from './EditExtractorConverters';
import EditExtractorConfiguration from './EditExtractorConfiguration';
import ExtractorExampleMessage from './ExtractorExampleMessage';

const StaticField = ({ label, text }: { label: string; text: string }) => (
  <FormGroup>
    <Col componentClass={ControlLabel} md={2}>
      {label}
    </Col>
    <Col md={10}>
      <FormControl.Static>{text}</FormControl.Static>
    </Col>
  </FormGroup>
);

type EditExtractorProps = {
  action: 'create' | 'edit';
  extractor: any;
  inputId: string;
  exampleMessage?: string;
  onSave: (...args: any[]) => void;
};

type EditExtractorState = {
  updatedExtractor?: EditExtractorProps['extractor'];
  conditionTestResult?: {};
  exampleMessage?: EditExtractorProps['exampleMessage'];
};

class EditExtractor extends React.Component<EditExtractorProps, EditExtractorState> {
  static defaultProps = {
    exampleMessage: undefined,
  };

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

  private targetField: Input;

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

  _onFieldChange = (key) => (event) => {
    const nextState: EditExtractorState = {};
    const { updatedExtractor } = this.state;

    updatedExtractor[key] = getValueFromInput(event.target);
    nextState.updatedExtractor = updatedExtractor;

    // Reset result of testing condition after a change in the input
    if (key === 'condition_value') {
      nextState.conditionTestResult = undefined;
    }

    this.setState(nextState);
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
    const tester = updatedExtractor.condition_type === 'string' ? testContainsString : testRegex;
    const promise = tester(updatedExtractor.condition_value, exampleMessage);

    promise.then((result) => this.setState({ conditionTestResult: result.matched }));
  };

  _tryButtonDisabled = () => {
    const { updatedExtractor, exampleMessage } = this.state;

    return updatedExtractor.condition_value === '' || updatedExtractor.condition_value === undefined || !exampleMessage;
  };

  _getExtractorConditionControls = () => {
    const { conditionTestResult, updatedExtractor } = this.state;

    if (!updatedExtractor.condition_type || updatedExtractor.condition_type === 'none') {
      return <div />;
    }

    let conditionInputLabel;
    let conditionInputHelp;

    if (updatedExtractor.condition_type === 'string') {
      conditionInputLabel = i18n.t('edit.condition_string_label', { ns: 'extractors' });
      conditionInputHelp = i18n.t('edit.condition_string_help', { ns: 'extractors' });
    } else {
      conditionInputLabel = i18n.t('edit.condition_regex_label', { ns: 'extractors' });
      conditionInputHelp = i18n.t('edit.condition_regex_help', { ns: 'extractors' });
    }

    let inputStyle;

    if (conditionTestResult === true) {
      inputStyle = 'success';
      conditionInputHelp = i18n.t('edit.condition_matches', { ns: 'extractors' });
    } else if (conditionTestResult === false) {
      inputStyle = 'error';
      conditionInputHelp = i18n.t('edit.condition_no_match', { ns: 'extractors' });
    }

    return (
      <div>
        <Input
          id="condition_value"
          label={conditionInputLabel}
          bsStyle={inputStyle}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          help={conditionInputHelp}>
          <Row className="row-sm">
            <Col md={11}>
              <input
                type="text"
                id="condition_value"
                className="form-control"
                defaultValue={updatedExtractor.condition_value}
                onChange={this._onFieldChange('condition_value')}
                required
              />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._testCondition} disabled={this._tryButtonDisabled()}>
                {i18n.t('edit.try', { ns: 'extractors' })}
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

    saveExtractor(inputId, updatedExtractor).then(() => onSave());
  };

  render() {
    const { updatedExtractor, exampleMessage } = this.state;
    const { action } = this.props;
    const conditionTypeHelpMessage = i18n.t('edit.condition_type_help', { ns: 'extractors' });

    const cursorStrategyHelpMessage = (
      <span>
        <Trans i18nKey="edit.cursor_strategy_help" ns="extractors" components={{ em: <em /> }} />
      </span>
    );

    const targetFieldHelpMessage = (
      <span>
        <Trans i18nKey="edit.target_field_help" ns="extractors" components={{ b: <b />, em: <em /> }} />
      </span>
    );

    let storeAsFieldInput;

    // Grok and JSON extractors create their required fields, so no need to add an input for them
    if (
      updatedExtractor.type !== ExtractorUtils.ExtractorTypes.GROK &&
      updatedExtractor.type !== ExtractorUtils.ExtractorTypes.JSON
    ) {
      storeAsFieldInput = (
        <Input
          type="text"
          ref={(targetField) => {
            this.targetField = targetField;
          }}
          id="target_field"
          label={i18n.t('edit.store_as_field', { ns: 'extractors' })}
          defaultValue={updatedExtractor.target_field}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          onChange={this._onTargetFieldChange}
          required
          help={targetFieldHelpMessage}
        />
      );
    }

    return (
      <div>
        <Row className="content extractor-list">
          <Col md={12}>
            <h2>{i18n.t('edit.example_message', { ns: 'extractors' })}</h2>
            <Row style={{ marginTop: 5 }}>
              <Col md={12}>
                <ExtractorExampleMessage
                  field={updatedExtractor.source_field}
                  example={exampleMessage}
                  onExampleLoad={this._updateExampleMessage}
                />
              </Col>
            </Row>
            <h2>{i18n.t('edit.configuration', { ns: 'extractors' })}</h2>
            <Row>
              <Col md={8}>
                <form className="extractor-form form-horizontal" method="POST" onSubmit={this._saveExtractor}>
                  <StaticField
                    label={i18n.t('edit.extractor_type', { ns: 'extractors' })}
                    text={ExtractorUtils.getReadableExtractorTypeName(updatedExtractor.type)}
                  />
                  <StaticField label={i18n.t('edit.source_field', { ns: 'extractors' })} text={updatedExtractor.source_field} />

                  <EditExtractorConfiguration
                    extractorType={updatedExtractor.type}
                    configuration={updatedExtractor.extractor_config}
                    onChange={this._onConfigurationChange}
                    exampleMessage={exampleMessage}
                  />

                  <Input
                    id="condition-type"
                    label={i18n.t('edit.condition', { ns: 'extractors' })}
                    labelClassName="col-md-2"
                    wrapperClassName="col-md-10"
                    help={conditionTypeHelpMessage}>
                    <span>
                      <div className="radio">
                        <label htmlFor="condition_type_none">
                          <input
                            type="radio"
                            name="condition_type"
                            id="condition_type_none"
                            value="none"
                            onChange={this._onFieldChange('condition_type')}
                            defaultChecked={
                              !updatedExtractor.condition_type || updatedExtractor.condition_type === 'none'
                            }
                          />
                          {i18n.t('edit.condition_none', { ns: 'extractors' })}
                        </label>
                      </div>
                      <div className="radio">
                        <label htmlFor="condition_type_string">
                          <input
                            type="radio"
                            name="condition_type"
                            id="condition_type_string"
                            value="string"
                            onChange={this._onFieldChange('condition_type')}
                            defaultChecked={updatedExtractor.condition_type === 'string'}
                          />
                          {i18n.t('edit.condition_string', { ns: 'extractors' })}
                        </label>
                      </div>
                      <div className="radio">
                        <label htmlFor="condition_type_regex">
                          <input
                            type="radio"
                            name="condition_type"
                            id="condition_type_regex"
                            value="regex"
                            onChange={this._onFieldChange('condition_type')}
                            defaultChecked={updatedExtractor.condition_type === 'regex'}
                          />
                          {i18n.t('edit.condition_regex', { ns: 'extractors' })}
                        </label>
                      </div>
                    </span>
                  </Input>
                  {this._getExtractorConditionControls()}

                  {storeAsFieldInput}

                  <Input
                    id="extraction-strategy"
                    label={i18n.t('edit.extraction_strategy', { ns: 'extractors' })}
                    labelClassName="col-md-2"
                    wrapperClassName="col-md-10"
                    help={cursorStrategyHelpMessage}>
                    <span>
                      <label className="radio-inline" htmlFor="cursor_strategy_copy">
                        <input
                          type="radio"
                          name="cursor_strategy"
                          id="cursor_strategy_copy"
                          value="copy"
                          onChange={this._onFieldChange('cursor_strategy')}
                          defaultChecked={
                            !updatedExtractor.cursor_strategy || updatedExtractor.cursor_strategy === 'copy'
                          }
                        />
                        {i18n.t('edit.copy', { ns: 'extractors' })}
                      </label>
                      <label className="radio-inline" htmlFor="cursor_strategy_cut">
                        <input
                          type="radio"
                          name="cursor_strategy"
                          id="cursor_strategy_cut"
                          value="cut"
                          onChange={this._onFieldChange('cursor_strategy')}
                          defaultChecked={updatedExtractor.cursor_strategy === 'cut'}
                        />
                        {i18n.t('edit.cut', { ns: 'extractors' })}
                      </label>
                    </span>
                  </Input>

                  <Input
                    type="text"
                    id="title"
                    label={i18n.t('edit.title', { ns: 'extractors' })}
                    defaultValue={updatedExtractor.title}
                    labelClassName="col-md-2"
                    wrapperClassName="col-md-10"
                    onChange={this._onFieldChange('title')}
                    required
                    help={i18n.t('edit.title_help', { ns: 'extractors' })}
                  />

                  <div style={{ marginBottom: 20 }}>
                    <EditExtractorConverters
                      extractorType={updatedExtractor.type}
                      converters={updatedExtractor.converters}
                      onChange={this._onConverterChange}
                    />
                  </div>

                  <Row>
                    <Col mdOffset={2} md={10}>
                      <Button type="submit" bsStyle="primary">
                        {action === 'create' ? i18n.t('edit.create', { ns: 'extractors' }) : i18n.t('edit.update', { ns: 'extractors' })}
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
