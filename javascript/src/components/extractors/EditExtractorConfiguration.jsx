import React, {PropTypes} from 'react';
import {Col, Panel} from 'react-bootstrap';

import CopyInputExtractorConfiguration from './extractors_configuration/CopyInputExtractorConfiguration';
import GrokExtractorConfiguration from './extractors_configuration/GrokExtractorConfiguration';
import JSONExtractorConfiguration from './extractors_configuration/JSONExtractorConfiguration';
import RegexExtractorConfiguration from './extractors_configuration/RegexExtractorConfiguration';
import RegexReplaceExtractorConfiguration from './extractors_configuration/RegexReplaceExtractorConfiguration';
import SplitAndIndexExtractorConfiguration from './extractors_configuration/SplitAndIndexExtractorConfiguration';
import SubstringExtractorConfiguration from './extractors_configuration/SubstringExtractorConfiguration';

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
    let extractorConfiguration;

    switch (this.props.extractorType) {
    case 'copy_input':
      extractorConfiguration = <CopyInputExtractorConfiguration/>;
      break;
    case 'grok':
      extractorConfiguration = (
        <GrokExtractorConfiguration configuration={this.props.configuration}
                                    exampleMessage={this.props.exampleMessage}
                                    onChange={this.props.onChange}
                                    onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'json':
      extractorConfiguration = (
        <JSONExtractorConfiguration configuration={this.props.configuration}
                                    exampleMessage={this.props.exampleMessage}
                                    onChange={this.props.onChange}
                                    onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'regex':
      extractorConfiguration = (
        <RegexExtractorConfiguration configuration={this.props.configuration}
                                     exampleMessage={this.props.exampleMessage}
                                     onChange={this.props.onChange}
                                     onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'regex_replace':
      extractorConfiguration = (
        <RegexReplaceExtractorConfiguration configuration={this.props.configuration}
                                            exampleMessage={this.props.exampleMessage}
                                            onChange={this.props.onChange}
                                            onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'substring':
      extractorConfiguration = (
        <SubstringExtractorConfiguration configuration={this.props.configuration}
                                         exampleMessage={this.props.exampleMessage}
                                         onChange={this.props.onChange}
                                         onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
      break;
    case 'split_and_index':
      extractorConfiguration = (
        <SplitAndIndexExtractorConfiguration configuration={this.props.configuration}
                                             exampleMessage={this.props.exampleMessage}
                                             onChange={this.props.onChange}
                                             onExtractorPreviewLoad={this._onExtractorPreviewLoad}/>
      );
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
        {extractorConfiguration}
        {extractorPreview}
      </div>
    );
  },
});

export default EditExtractorConfiguration;
