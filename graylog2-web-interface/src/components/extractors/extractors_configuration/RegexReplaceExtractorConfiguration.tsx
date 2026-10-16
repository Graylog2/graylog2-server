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
import { Col, Row, Button, Input } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import UserNotification from 'util/UserNotification';
import { getValueFromInput } from 'util/FormsUtils';
import { testRegexReplace } from 'api/tools';

// Example values of the format, not translated.
const REGEX_EXAMPLE = '^.*string(.+)$';
const REPLACEMENT_EXAMPLE = '$1';

const DocsLink = ({ children = undefined }: { children?: React.ReactNode }) => (
  <DocumentationLink page={DocsHelper.PAGES.EXTRACTORS} text={children} />
);

type Props = {
  configuration: any;
  exampleMessage?: string;
  onChange: (...args: any[]) => void;
  onExtractorPreviewLoad: (...args: any[]) => void;
} & WithTranslation;

class RegexReplaceExtractorConfiguration extends React.Component<Props, { trying: boolean }> {
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

    const { configuration } = this.props;
    const promise = testRegexReplace(
      configuration.regex,
      configuration.replacement,
      configuration.replace_all,
      this.props.exampleMessage,
    );

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

  _isTryButtonDisabled = () =>
    this.state.trying ||
    !this.props.configuration.regex ||
    !this.props.configuration.replacement ||
    !this.props.exampleMessage;

  render() {
    const { t } = this.props;
    const regexHelpMessage = (
      <span>
        <Trans t={t} i18nKey="regex_replace.regex_help" components={{ docsLink: <DocsLink /> }} />
      </span>
    );

    const replacementHelpMessage = (
      <span>
        <Trans
          t={t}
          i18nKey="regex_replace.replacement_help"
          components={{
            matcherLink: (
              // The link text comes from the translation.
              // eslint-disable-next-line jsx-a11y/anchor-has-content, jsx-a11y/control-has-associated-label
              <a
                target="_blank"
                href="https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html#replaceAll(java.lang.String)"
                rel="noreferrer"
              />
            ),
          }}
        />
      </span>
    );

    return (
      <div>
        <Input
          type="text"
          id="regex"
          label={t('regex.label')}
          labelClassName="col-md-2"
          placeholder={REGEX_EXAMPLE}
          onChange={this._onChange('regex')}
          wrapperClassName="col-md-10"
          defaultValue={this.props.configuration.regex}
          required
          help={regexHelpMessage}
        />

        <Input
          type="text"
          id="replacement"
          label={t('regex_replace.replacement')}
          labelClassName="col-md-2"
          placeholder={REPLACEMENT_EXAMPLE}
          onChange={this._onChange('replacement')}
          wrapperClassName="col-md-10"
          defaultValue={this.props.configuration.replacement}
          required
          help={replacementHelpMessage}
        />

        <Input
          type="checkbox"
          id="replace_all"
          label={t('regex_replace.replace_all')}
          wrapperClassName="col-md-offset-2 col-md-10"
          defaultChecked={this.props.configuration.replace_all}
          onChange={this._onChange('replace_all')}
          help={t('regex_replace.replace_all_help')}
        />

        <Row>
          <Col mdOffset={2} md={10}>
            <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
              {this.state.trying ? <Icon name="progress_activity" spin /> : t('edit.try')}
            </Button>
          </Col>
        </Row>
      </div>
    );
  }
}

export default withTranslation('extractors')(RegexReplaceExtractorConfiguration);
