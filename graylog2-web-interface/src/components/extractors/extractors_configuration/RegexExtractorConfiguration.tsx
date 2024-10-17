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

import { Icon } from 'components/common';
import { Row, Col, Button, Input } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import FormUtils from 'util/FormsUtils';
import ToolsStore from 'stores/tools/ToolsStore';

type RegexExtractorConfigurationProps = {
  configuration: any;
  exampleMessage?: string;
  onChange: (...args: any[]) => void;
  onExtractorPreviewLoad: (...args: any[]) => void;
};

class RegexExtractorConfiguration extends React.Component<RegexExtractorConfigurationProps, {
  [key: string]: any;
}> {
  state = {
    trying: false,
  };

  _onChange = (key) => (event) => {
    this.props.onExtractorPreviewLoad(undefined);
    const newConfig = this.props.configuration;

    newConfig[key] = FormUtils.getValueFromInput(event.target);
    this.props.onChange(newConfig);
  };

  _onTryClick = () => {
    this.setState({ trying: true });

    const promise = ToolsStore.testRegex(this.props.configuration.regex_value, this.props.exampleMessage);

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

  _isTryButtonDisabled = () => this.state.trying || !this.props.configuration.regex_value || !this.props.exampleMessage;

  render() {
    const helpMessage = (
      <span>
        The regular expression used for extraction. First matcher group is used.{' '}
        Learn more in the <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text="documentation" />.
      </span>
    );

    return (
      <div>
        <Input id="regex-value-input"
               label="Regular expression"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <input type="text"
                     id="regex_value"
                     className="form-control"
                     defaultValue={this.props.configuration.regex_value}
                     placeholder="^.*string(.+)$"
                     onChange={this._onChange('regex_value')}
                     required />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <Icon name="progress_activity" spin /> : 'Try'}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  }
}

export default RegexExtractorConfiguration;
