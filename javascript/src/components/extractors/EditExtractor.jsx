import React, {PropTypes} from 'react';
import {Row, Col, Input, Button, FormControls, Panel} from 'react-bootstrap';
import {LinkContainer} from 'react-router-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import ExtractorExampleMessage from './ExtractorExampleMessage';
import ExtractorUtils from 'util/ExtractorUtils';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';

import ToolsStore from 'stores/tools/ToolsStore';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

const EditExtractor = React.createClass({
  propTypes: {
    extractor: PropTypes.object,
    inputId: PropTypes.string.isRequired,
    exampleMessage: PropTypes.string,
  },
  getDefaultProps() {
    return {
      exampleMessage: 'This is an example message, please change me!',
    };
  },
  getInitialState() {
    return {
      updatedExtractor: this.props.extractor,
      conditionTestResult: undefined,
    };
  },
  _onFieldChange(key) {
    return (event) => {
      const nextState = {};
      const updatedExtractor = this.state.updatedExtractor;
      updatedExtractor[key] = event.target.value;
      nextState.updatedExtractor = updatedExtractor;

      // Reset result of testing condition after a change in the input
      if (key === 'condition_value') {
        nextState.conditionTestResult = undefined;
      }

      this.setState(nextState);
    };
  },
  _testCondition() {
    const promise = ToolsStore.testRegex(this.state.updatedExtractor.condition_value, this.props.exampleMessage);
    promise.then(result => this.setState({conditionTestResult: result.matched}));
  },
  _getExtractorConditionControls() {
    let conditionInput;

    if (this.state.updatedExtractor.condition_type !== 'none') {
      let conditionInputLabel;
      let conditionInputHelp;

      if (this.state.updatedExtractor.condition_type === 'string') {
        conditionInputLabel = 'Field contains string';
        conditionInputHelp = 'Type a string that the field should contain in order to attempt the extraction.';
      } else {
        conditionInputLabel = 'Field matches regular expression';
        conditionInputHelp = 'Type a regular expression that the field should contain in order to attempt the extraction.';
      }

      let inputStyle;
      if (this.state.conditionTestResult === true) {
        inputStyle = 'success';
        conditionInputHelp = 'Matches! Extractor would run against this example.';
      } else if (this.state.conditionTestResult === false) {
        inputStyle = 'error';
        conditionInputHelp = 'Does not match! Extractor would not run.';
      }

      conditionInput = (
        <div>
          <Input id="condition_value" label={conditionInputLabel}
                 bsStyle={inputStyle}
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 help={conditionInputHelp}>
            <Row className="row-sm">
              <Col md={11}>
                <input type="text" id="condition_value" className="form-control"
                       defaultValue={this.state.updatedExtractor.condition_value}
                       onChange={this._onFieldChange('condition_value')} required/>
              </Col>
              <Col md={1} className="text-right">
                <Button bsStyle="info" onClick={this._testCondition} disabled={this.state.updatedExtractor.condition_value === ''}>
                  Try
                </Button>
              </Col>
            </Row>
          </Input>
        </div>
      );
    }

    return conditionInput;
  },
  _getExtractorControls(extractorType, config) {
    const controls = [];
    let helpMessage;

    switch (extractorType) {
    case 'copy_input':
      controls.push(
        <div key="copyInputInfo" className="form-group">
          <div className="col-md-offset-2 col-md-10">
            <Panel bsStyle="info" style={{marginBottom: 0}}>
              The entire input will be copied verbatim.
            </Panel>
          </div>
        </div>
      );
      break;
    case 'grok':
      helpMessage = (
        <span>
          Matches the field against the current Grok pattern list, use <b>{'%{PATTERN-NAME}'}</b> to refer to a{' '}
          <LinkContainer to={Routes.SYSTEM.GROKPATTERNS}><a>stored pattern</a></LinkContainer>.
        </span>
      );
      controls.push(
        <div key="grokControls">
          <Input type="text" id="grok_pattern" label="Grok pattern" labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={config.grok_pattern}
                 help={helpMessage}/>
        </div>
      );

      // TODO: try
      break;
    case 'json':
      controls.push(
        <div key="jsonControls">
          <Input type="checkbox" label="Flatten structures" wrapperClassName="col-md-offset-2 col-md-10"
                 defaultChecked={config.flatten}
                 help="Whether to flatten JSON objects into a single message field or to expand into multiple fields."/>
          <Input type="text" label="List item separator" id="list_separator" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.list_separator} required
                 help="What string to use to concatenate items of a JSON list."/>
          <Input type="text" label="Key separator" id="key_separator" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.key_separator} required
                 help={<span>What string to use to concatenate different keys of a nested JSON object (only used if <em>not</em> flattened).</span>}/>
          <Input type="text" label="Key/value separator" id="kv_separator" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.kv_separator} required
                 help="What string to use when concatenating key/value pairs of a JSON object (only used if flattened)."/>
        </div>
      );

      // TODO: try
      break;
    case 'regex':
      helpMessage = (
        <span>
          The regular expression used for extraction. First matcher group is used.{' '}
          Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
        </span>
      );
      controls.push(
        <div key="regexControls">
          <Input type="text" label="Regular expression" id="regex_value" labelClassName="col-md-2"
                 placeholder="^.*string(.+)$"
                 wrapperClassName="col-md-10" defaultValue={config.regex_value} required
                 help={helpMessage}/>
        </div>
      );

      // TODO: try
      break;
    case 'regex_replace':
      helpMessage = (
        <span>
          The regular expression used for extraction.{' '}
          Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation"/>.
        </span>
      );
      controls.push(
        <div key="regexReplaceControls">
          <Input type="text" label="Regular expression" id="regex" labelClassName="col-md-2"
                 placeholder="^.*string(.+)$"
                 wrapperClassName="col-md-10" defaultValue={config.regex} required
                 help={helpMessage}/>
          <Input type="text" label="Replacement" id="replacement" labelClassName="col-md-2"
                 placeholder="$1"
                 wrapperClassName="col-md-10" defaultValue={config.replacement} required
                 help={<span>The replacement used for the matching text. Please refer to the <a target="_blank" href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html#replaceAll(java.lang.String)">Matcher</a> API documentation for the possible options.</span>}/>
          <Input type="checkbox" label="Replace all occurrences of the pattern"
                 wrapperClassName="col-md-offset-2 col-md-10"
                 defaultChecked={config.replace_all}
                 help="Whether to replace all occurrences of the given pattern or only the first occurrence."/>
        </div>
      );

      // TODO: try
      break;
    case 'substring':
      controls.push(
        <div key="substringControls">
          <Input type="text" label="Begin index" id="begin_index" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.begin_index} required
                 help="Character position from where to start extracting. (Inclusive)"/>
          <Input type="text" label="End index" id="end_index" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.end_index} required
                 help={<span>Where to end extracting. (Exclusive) <strong>Example:</strong> <em>1,5</em> cuts <em>love</em> from the string <em>ilovelogs</em>.</span>}/>
        </div>
      );

      // TODO: try
      break;
    case 'split_and_index':
      controls.push(
        <div key="splitAndIndexControls">
          <Input type="text" label="Split by" id="split_by" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.split_by} required
                 help={<span>What character to split on. <strong>Example:</strong> A whitespace character will split <em>foo bar baz</em> to <em>[foo,bar,baz]</em>.</span>}/>
          <Input type="text" label="Target index" id="index" labelClassName="col-md-2"
                 wrapperClassName="col-md-10" defaultValue={config.index} required
                 help={<span>What part of the split string to you want to use? <strong>Example:</strong> <em>2</em> selects <em>bar</em> from <em>foo bar baz</em> when split by whitespace.</span>}/>
        </div>
      );

      // TODO: try
      break;
    default:
      console.warn(`Unsupported extractor type ${extractorType}`);
    }

    return controls;
  },
  _updateExtractor(event) {
    event.preventDefault();
    ExtractorsActions.update.triggerPromise(this.props.inputId, this.state.updatedExtractor);
  },
  render() {
    // TODO:
    // - Add converters
    // - Load recent message from input

    return (
      <div>
        <Row className="content extractor-list">
          <Col md={12}>
            <h2>Example message</h2>
            <Row style={{marginTop: 5}}>
              <Col md={12}>
                <ExtractorExampleMessage field={this.state.updatedExtractor.target_field}
                                         example={this.props.exampleMessage}/>
              </Col>
            </Row>
            <h2>Extractor configuration</h2>
            <Row>
              <Col md={8}>
                <form className="extractor-form form-horizontal" method="POST" onSubmit={this._updateExtractor}>
                  <FormControls.Static label="Extractor type"
                                       value={ExtractorUtils.getReadableExtractorTypeName(this.state.updatedExtractor.type)}
                                       labelClassName="col-md-2" wrapperClassName="col-md-10"/>
                  <FormControls.Static label="Source field" value={this.state.updatedExtractor.source_field}
                                       labelClassName="col-md-2" wrapperClassName="col-md-10"/>

                  {this._getExtractorControls(this.state.updatedExtractor.type, this.state.updatedExtractor.extractor_config)}

                  <Input label="Condition" labelClassName="col-md-2" wrapperClassName="col-md-10"
                         help="Extracting only from messages that match a certain condition helps you avoiding wrong or unnecessary extractions and can also save CPU resources.">
                    <div className="radio">
                      <label>
                        <input type="radio" name="condition_type" value="none"
                               onChange={this._onFieldChange('condition_type')}
                               defaultChecked={this.state.updatedExtractor.condition_type === 'none'}/>
                        Always try to extract
                      </label>
                    </div>
                    <div className="radio">
                      <label>
                        <input type="radio" name="condition_type" value="string"
                               onChange={this._onFieldChange('condition_type')}
                               defaultChecked={this.state.updatedExtractor.condition_type === 'string'}/>
                        Only attempt extraction if field contains string
                      </label>
                    </div>
                    <div className="radio">
                      <label>
                        <input type="radio" name="condition_type" value="regex"
                               onChange={this._onFieldChange('condition_type')}
                               defaultChecked={this.state.updatedExtractor.condition_type === 'regex'}/>
                        Only attempt extraction if field matches regular expression
                      </label>
                    </div>
                  </Input>
                  {this._getExtractorConditionControls()}

                  <Input type="text" id="target_field" label="Store as field"
                         defaultValue={this.state.updatedExtractor.target_field}
                         labelClassName="col-md-2"
                         wrapperClassName="col-md-10"
                         onChange={this._onFieldChange('target_field')}
                         required
                         help={<span>Choose a field name. The extracted value will be stored in it. Call it <em>http_response_code</em> for example if you are extracting a HTTP response code.</span>}/>


                  <Input label="Extraction strategy" labelClassName="col-md-2" wrapperClassName="col-md-10"
                         help={<span>Do you want to copy or cut from source? You cannot use the cutting feature on standard fields like <em>message</em> and <em>source</em>.</span>}>
                    <label className="radio-inline">
                      <input type="radio" name="cursor_strategy" value="copy"
                             onChange={this._onFieldChange('cursor_strategy')}
                             defaultChecked={this.state.updatedExtractor.cursor_strategy === 'copy'}/>
                      Copy
                    </label>
                    <label className="radio-inline">
                      <input type="radio" name="cursor_strategy" value="cut"
                             onChange={this._onFieldChange('cursor_strategy')}
                             defaultChecked={this.state.updatedExtractor.cursor_strategy === 'cut'}/>
                      Cut
                    </label>
                  </Input>

                  <Input type="text" id="title" label="Extractor title"
                         defaultValue={this.state.updatedExtractor.title}
                         labelClassName="col-md-2"
                         wrapperClassName="col-md-10"
                         onChange={this._onFieldChange('title')}
                         required
                         help="A descriptive name for this extractor."/>

                  <Input wrapperClassName="col-md-offset-2 col-md-10">
                    <Button type="submit" bsStyle="success">Update extractor</Button>
                  </Input>
                </form>
              </Col>
            </Row>
          </Col>
        </Row>
      </div>
    );
  },
});

export default EditExtractor;
