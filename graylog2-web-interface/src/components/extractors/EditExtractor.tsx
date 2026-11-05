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
} & WithTranslation;

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
    const { t } = this.props;

    if (!updatedExtractor.condition_type || updatedExtractor.condition_type === 'none') {
      return <div />;
    }

    let conditionInputLabel;
    let conditionInputHelp;

    if (updatedExtractor.condition_type === 'string') {
      conditionInputLabel = t('edit.condition_string_label');
      conditionInputHelp = t('edit.condition_string_help');
    } else {
      conditionInputLabel = t('edit.condition_regex_label');
      conditionInputHelp = t('edit.condition_regex_help');
    }

    let inputStyle;

    if (conditionTestResult === true) {
      inputStyle = 'success';
      conditionInputHelp = t('edit.condition_matches');
    } else if (conditionTestResult === false) {
      inputStyle = 'error';
      conditionInputHelp = t('edit.condition_no_match');
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
                {t('edit.try')}
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
    const { action, t } = this.props;
    const conditionTypeHelpMessage = t('edit.condition_type_help');

    const cursorStrategyHelpMessage = (
      <span>
        <Trans t={t} i18nKey="edit.cursor_strategy_help" components={{ em: <em /> }} />
      </span>
    );

    const targetFieldHelpMessage = (
      <span>
        <Trans t={t} i18nKey="edit.target_field_help" components={{ b: <b />, em: <em /> }} />
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
          label={t('edit.store_as_field')}
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
            <h2>{t('edit.example_message')}</h2>
            <Row style={{ marginTop: 5 }}>
              <Col md={12}>
                <ExtractorExampleMessage
                  field={updatedExtractor.source_field}
                  example={exampleMessage}
                  onExampleLoad={this._updateExampleMessage}
                />
              </Col>
            </Row>
            <h2>{t('edit.configuration')}</h2>
            <Row>
              <Col md={8}>
                <form className="extractor-form form-horizontal" method="POST" onSubmit={this._saveExtractor}>
                  <StaticField
                    label={t('edit.extractor_type')}
                    text={ExtractorUtils.getReadableExtractorTypeName(updatedExtractor.type)}
                  />
                  <StaticField label={t('edit.source_field')} text={updatedExtractor.source_field} />

                  <EditExtractorConfiguration
                    extractorType={updatedExtractor.type}
                    configuration={updatedExtractor.extractor_config}
                    onChange={this._onConfigurationChange}
                    exampleMessage={exampleMessage}
                  />

                  <Input
                    id="condition-type"
                    label={t('edit.condition')}
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
                          {t('edit.condition_none')}
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
                          {t('edit.condition_string')}
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
                          {t('edit.condition_regex')}
                        </label>
                      </div>
                    </span>
                  </Input>
                  {this._getExtractorConditionControls()}

                  {storeAsFieldInput}

                  <Input
                    id="extraction-strategy"
                    label={t('edit.extraction_strategy')}
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
                        {t('edit.copy')}
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
                        {t('edit.cut')}
                      </label>
                    </span>
                  </Input>

                  <Input
                    type="text"
                    id="title"
                    label={t('edit.title')}
                    defaultValue={updatedExtractor.title}
                    labelClassName="col-md-2"
                    wrapperClassName="col-md-10"
                    onChange={this._onFieldChange('title')}
                    required
                    help={t('edit.title_help')}
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
                        {action === 'create' ? t('edit.create') : t('edit.update')}
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

export default withTranslation('extractors')(EditExtractor);
