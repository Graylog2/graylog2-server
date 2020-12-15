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

import { Col, Row, Button } from 'components/graylog';
import { Icon } from 'components/common';
import { Input } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import FormUtils from 'util/FormsUtils';
import StoreProvider from 'injection/StoreProvider';

const ToolsStore = StoreProvider.getStore('Tools');

class RegexReplaceExtractorConfiguration extends React.Component {
  static propTypes = {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  };

  state = {
    trying: false,
  };

  _onChange = (key) => {
    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      const newConfig = this.props.configuration;

      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(newConfig);
    };
  };

  _onTryClick = () => {
    this.setState({ trying: true });

    const { configuration } = this.props;
    const promise = ToolsStore.testRegexReplace(configuration.regex, configuration.replacement,
      configuration.replace_all, this.props.exampleMessage);

    promise.then((result) => {
      if (!result.matched) {
        UserNotification.warning('Regular expression did not match.');

        return;
      }

      if (!result.match) {
        UserNotification.warning('Regular expression does not contain any matcher group to extract.');

        return;
      }

      const preview = (result.match.match ? <samp>{result.match.match}</samp> : '');

      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  };

  _isTryButtonDisabled = () => {
    return this.state.trying || !this.props.configuration.regex || !this.props.configuration.replacement || !this.props.exampleMessage;
  };

  render() {
    const regexHelpMessage = (
      <span>
        The regular expression used for extraction.{' '}
        Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation" />.
      </span>
    );

    const replacementHelpMessage = (
      <span>The replacement used for the matching text. Please refer to the{' '}
        <a target="_blank"
           href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html#replaceAll(java.lang.String)">Matcher
        </a>{' '}
        API documentation for the possible options.
      </span>
    );

    return (
      <div>
        <Input type="text"
               id="regex"
               label="Regular expression"
               labelClassName="col-md-2"
               placeholder="^.*string(.+)$"
               onChange={this._onChange('regex')}
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.regex}
               required
               help={regexHelpMessage} />

        <Input type="text"
               id="replacement"
               label="Replacement"
               labelClassName="col-md-2"
               placeholder="$1"
               onChange={this._onChange('replacement')}
               wrapperClassName="col-md-10"
               defaultValue={this.props.configuration.replacement}
               required
               help={replacementHelpMessage} />

        <Input type="checkbox"
               id="replace_all"
               label="Replace all occurrences of the pattern"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={this.props.configuration.replace_all}
               onChange={this._onChange('replace_all')}
               help="Whether to replace all occurrences of the given pattern or only the first occurrence." />

        <Row>
          <Col mdOffset={2} md={10}>
            <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
              {this.state.trying ? <Icon name="spinner" spin /> : 'Try'}
            </Button>
          </Col>
        </Row>
      </div>
    );
  }
}

export default RegexReplaceExtractorConfiguration;
