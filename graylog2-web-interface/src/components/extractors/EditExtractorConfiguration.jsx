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

import { Col, Panel } from 'components/graylog';
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

class EditExtractorConfiguration extends React.Component {
  static propTypes = {
    extractorType: PropTypes.oneOf(ExtractorUtils.EXTRACTOR_TYPES).isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    exampleMessage: PropTypes.string,
  };

  state = {
    extractorPreview: undefined,
  };

  _onExtractorPreviewLoad = (extractorPreviewNode) => {
    this.setState({ extractorPreview: extractorPreviewNode });
  };

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
  }
}

export default EditExtractorConfiguration;
