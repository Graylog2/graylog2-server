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
import styled, { css } from 'styled-components';
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Well } from 'components/bootstrap';
import FieldValue from 'components/common/message/details/fields/FieldValue';

import MessageLoader from './MessageLoader';

const NewExampleWell = styled(Well)(
  ({ theme }) => css`
    margin-bottom: 5px;
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.body};
    white-space: pre-wrap;
    overflow-wrap: break-word;
  `,
);

const NoExample = styled.div`
  margin-top: 15px;
  margin-bottom: 12px;
`;

type ExtractorExampleMessageProps = {
  field: string;
  example?: string;
  onExampleLoad?: (...args: any[]) => void;
} & WithTranslation;

class ExtractorExampleMessage extends React.Component<
  ExtractorExampleMessageProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    example: '',
    onExampleLoad: () => {},
  };

  _onExampleLoad = (message) => {
    const { field, onExampleLoad } = this.props;

    const newExample = message.fields[field];

    onExampleLoad(newExample);
  };

  render() {
    const { example, field, t } = this.props;
    const originalMessage = (
      <span id="xtrc-original-example" style={{ display: 'none' }}>
        {example}
      </span>
    );
    let messagePreview;

    if (example) {
      messagePreview = (
        <NewExampleWell bsSize="small">
          <span id="xtrc-example">
            <FieldValue value={example} />
          </span>
        </NewExampleWell>
      );
    } else {
      messagePreview = (
        <NoExample className="alert alert-warning">{t('example_message.not_loaded', { field })}</NoExample>
      );
    }

    return (
      <div>
        {originalMessage}
        {messagePreview}
        <MessageLoader onMessageLoaded={this._onExampleLoad} />
      </div>
    );
  }
}

export default withTranslation('extractors')(ExtractorExampleMessage);
