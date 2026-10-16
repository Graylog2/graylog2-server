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

import { Icon } from 'components/common';
import { Row, Col, Button, Input } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import { getValueFromInput } from 'util/FormsUtils';
import { testRegex } from 'api/tools';

// Example value of the format, not translated.
const REGEX_EXAMPLE = '^.*string(.+)$';

const DocsLink = ({ children = undefined }: { children?: React.ReactNode }) => (
  <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text={children} />
);

type Props = {
  configuration: any;
  exampleMessage?: string;
  onChange: (...args: any[]) => void;
  onExtractorPreviewLoad: (...args: any[]) => void;
} & WithTranslation;

class RegexExtractorConfiguration extends React.Component<Props, { trying: boolean }> {
  static defaultProps = {
    exampleMessage: undefined,
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      trying: false,
    };
  }

  _onChange = (key) => (event) => {
    this.props.onExtractorPreviewLoad(undefined);
    const newConfig = this.props.configuration;

    newConfig[key] = getValueFromInput(event.target);
    this.props.onChange(newConfig);
  };

  _onTryClick = () => {
    this.setState({ trying: true });

    const promise = testRegex(this.props.configuration.regex_value, this.props.exampleMessage);

    promise.then((result) => {
      if (!result.matched) {
        UserNotification.warning(this.props.t('regex.no_match'));

        return;
      }

      if (!result.match) {
        UserNotification.warning(this.props.t('regex.no_group'));

        return;
      }

      const preview = result.match.match ? <samp>{result.match.match}</samp> : '';

      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  };

  _isTryButtonDisabled = () => this.state.trying || !this.props.configuration.regex_value || !this.props.exampleMessage;

  render() {
    const { t } = this.props;
    const helpMessage = (
      <span>
        <Trans t={t} i18nKey="regex.help" components={{ docsLink: <DocsLink /> }} />
      </span>
    );

    return (
      <div>
        <Input
          id="regex-value-input"
          label={t('regex.label')}
          labelClassName="col-md-2"
          wrapperClassName="col-md-10"
          help={helpMessage}>
          <Row className="row-sm">
            <Col md={11}>
              <input
                type="text"
                id="regex_value"
                className="form-control"
                defaultValue={this.props.configuration.regex_value}
                placeholder={REGEX_EXAMPLE}
                aria-label={t('regex.label')}
                onChange={this._onChange('regex_value')}
                required
              />
            </Col>
            <Col md={1} className="text-right">
              <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
                {this.state.trying ? <Icon name="progress_activity" spin /> : t('edit.try')}
              </Button>
            </Col>
          </Row>
        </Input>
      </div>
    );
  }
}

export default withTranslation('extractors')(RegexExtractorConfiguration);
