import React, {PropTypes} from 'react';
import {Col, Input, Panel} from 'react-bootstrap';

import CopyInputExtractorConfiguration from './extractors_configuration/CopyInputExtractorConfiguration';
import GrokExtractorConfiguration from './extractors_configuration/GrokExtractorConfiguration';
import JSONExtractorConfiguration from './extractors_configuration/JSONExtractorConfiguration';
import RegexExtractorConfiguration from './extractors_configuration/RegexExtractorConfiguration';
import RegexReplaceExtractorConfiguration from './extractors_configuration/RegexReplaceExtractorConfiguration';

import ExtractorUtils from 'util/ExtractorUtils';

const EditExtractorConfiguration = React.createClass({
  propTypes: {
    extractorType: PropTypes.oneOf(ExtractorUtils.EXTRACTOR_TYPES).isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    exampleMessage: PropTypes.string.isRequired,
  },
  getInitialState() {
    return {
      extractorPreview: undefined,
    };
  },
  _onExtractorPreviewLoad(extractorPreviewNode) {
    this.setState({extractorPreview: extractorPreviewNode});
  },
  render() {
    let control;
    const controls = [];

    switch (this.props.extractorType) {
    case 'copy_input':
      control = <CopyInputExtractorConfiguration/>;
      break;
    case 'grok':
      control = (
        <GrokExtractorConfiguration configuration={this.props.configuration}
                                    exampleMessage={this.props.exampleMessage}
                                    onChange={this.props.onChange}
                                    onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'json':
      control = (
        <JSONExtractorConfiguration configuration={this.props.configuration}
                                    exampleMessage={this.props.exampleMessage}
                                    onChange={this.props.onChange}
                                    onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'regex':
      control = (
        <RegexExtractorConfiguration configuration={this.props.configuration}
                                     exampleMessage={this.props.exampleMessage}
                                     onChange={this.props.onChange}
                                     onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'regex_replace':
      control = (
        <RegexReplaceExtractorConfiguration configuration={this.props.configuration}
                                            exampleMessage={this.props.exampleMessage}
                                            onChange={this.props.onChange}
                                            onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'substring':
      controls.push(
        <div key="substringControls">
          <Input type="number"
                 id="begin_index"
                 label="Begin index"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.begin_index}
                 onChange={this.props.onChange('begin_index')}
                 min="0"
                 required
                 help="Character position from where to start extracting. (Inclusive)"/>

          <Input type="number"
                 id="end_index"
                 label="End index"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.end_index}
                 onChange={this.props.onChange('end_index')}
                 required
                 help={<span>Where to end extracting. (Exclusive) <strong>Example:</strong> <em>1,5</em> cuts <em>love</em> from the string <em>ilovelogs</em>.</span>}/>
        </div>
      );

      // TODO: try
      break;
    case 'split_and_index':
      controls.push(
        <div key="splitAndIndexControls">
          <Input type="text"
                 id="split_by"
                 label="Split by"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.split_by}
                 onChange={this.props.onChange('split_by')}
                 required
                 help={<span>What character to split on. <strong>Example:</strong> A whitespace character will split <em>foo bar baz</em> to <em>[foo,bar,baz]</em>.</span>}/>

          <Input type="number"
                 id="index"
                 label="Target index"
                 labelClassName="col-md-2"
                 wrapperClassName="col-md-10"
                 defaultValue={this.props.configuration.index}
                 onChange={this.props.onChange('index')}
                 required
                 help={<span>What part of the split string to you want to use? <strong>Example:</strong> <em>2</em> selects <em>bar</em> from <em>foo bar baz</em> when split by whitespace.</span>}/>
        </div>
      );

      // TODO: try
      break;
    default:
      console.warn(`Unsupported extractor type ${this.props.extractorType}`);
    }

    let extractorPreview;

    if (this.state.extractorPreview) {
      extractorPreview = (
        <div className="form-group">
          <Col md={10} mdOffset={2}>
            <Panel header="Extractor preview" bsStyle="info">
              {this.state.extractorPreview}
            </Panel>
          </Col>
        </div>
      );
    }

    return (
      <div>
        {control || controls}
        {extractorPreview}
      </div>
    );
  },
});

export default EditExtractorConfiguration;
