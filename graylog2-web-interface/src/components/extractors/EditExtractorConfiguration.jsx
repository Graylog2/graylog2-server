import React, { PropTypes } from 'react';
import { Col, Panel } from 'react-bootstrap';

import {
  CopyInputExtractorConfiguration,
  GrokExtractorConfiguration,
  JSONExtractorConfiguration,
  RegexExtractorConfiguration,
  RegexReplaceExtractorConfiguration,
  SplitAndIndexExtractorConfiguration,
  SubstringExtractorConfiguration,
  LookupTableExtractorConfiguration,
} from 'components/extractors/extractors_configuration';

import ExtractorUtils from 'util/ExtractorUtils';

const EditExtractorConfiguration = React.createClass({
  propTypes: {
    extractorType: PropTypes.oneOf(ExtractorUtils.EXTRACTOR_TYPES).isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    exampleMessage: PropTypes.string,
  },
  getInitialState() {
    return {
      extractorPreview: undefined,
    };
  },
  _onExtractorPreviewLoad(extractorPreviewNode) {
    this.setState({ extractorPreview: extractorPreviewNode });
  },
  render() {
    let extractorConfiguration;

    switch (this.props.extractorType) {
      case ExtractorUtils.ExtractorTypes.COPY_INPUT:
        extractorConfiguration = <CopyInputExtractorConfiguration />;
        break;
      case ExtractorUtils.ExtractorTypes.GROK:
        extractorConfiguration = (
          <GrokExtractorConfiguration configuration={this.props.configuration}
                                    exampleMessage={this.props.exampleMessage}
                                    onChange={this.props.onChange}
                                    onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
      );
        break;
      case ExtractorUtils.ExtractorTypes.JSON:
        extractorConfiguration = (
          <JSONExtractorConfiguration configuration={this.props.configuration}
                                    exampleMessage={this.props.exampleMessage}
                                    onChange={this.props.onChange}
                                    onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
      );
        break;
      case ExtractorUtils.ExtractorTypes.REGEX:
        extractorConfiguration = (
          <RegexExtractorConfiguration configuration={this.props.configuration}
                                     exampleMessage={this.props.exampleMessage}
                                     onChange={this.props.onChange}
                                     onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
      );
        break;
      case ExtractorUtils.ExtractorTypes.REGEX_REPLACE:
        extractorConfiguration = (
          <RegexReplaceExtractorConfiguration configuration={this.props.configuration}
                                            exampleMessage={this.props.exampleMessage}
                                            onChange={this.props.onChange}
                                            onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
      );
        break;
      case ExtractorUtils.ExtractorTypes.SUBSTRING:
        extractorConfiguration = (
          <SubstringExtractorConfiguration configuration={this.props.configuration}
                                         exampleMessage={this.props.exampleMessage}
                                         onChange={this.props.onChange}
                                         onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
      );
        break;
      case ExtractorUtils.ExtractorTypes.SPLIT_AND_INDEX:
        extractorConfiguration = (
          <SplitAndIndexExtractorConfiguration configuration={this.props.configuration}
                                             exampleMessage={this.props.exampleMessage}
                                             onChange={this.props.onChange}
                                             onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
      );
        break;
      case ExtractorUtils.ExtractorTypes.LOOKUP_TABLE:
        extractorConfiguration = (
          <LookupTableExtractorConfiguration configuration={this.props.configuration}
                                             exampleMessage={this.props.exampleMessage}
                                             onChange={this.props.onChange}
                                             onExtractorPreviewLoad={this._onExtractorPreviewLoad} />
        );
        break;
      default:
        console.warn(`Unsupported extractor type ${this.props.extractorType}`);
    }

    let extractorPreview;

    if (this.state.extractorPreview !== undefined) {
      extractorPreview = (
        <div className="form-group">
          <Col md={10} mdOffset={2}>
            <Panel header="Extractor preview" bsStyle="info">
              {this.state.extractorPreview === '' ? <em>Nothing will be extracted</em> : this.state.extractorPreview}
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
