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
import styled, { css } from 'styled-components';

import { Well } from 'components/graylog';

import MessageLoader from './MessageLoader';

const NewExampleWell = styled(Well)(({ theme }) => css`
  margin-bottom: 5px;
  font-family: ${theme.fonts.family.monospace};
  font-size: ${theme.fonts.size.body};
  white-space: pre-wrap;
  word-wrap: break-word;
`);

const NoExample = styled.div`
  margin-top: 15px;
  margin-bottom: 12px;
`;

class ExtractorExampleMessage extends React.Component {
  static propTypes = {
    field: PropTypes.string.isRequired,
    example: PropTypes.string,
    onExampleLoad: PropTypes.func,
  };

  static defaultProps = {
    example: '',
    onExampleLoad: () => {},
  }

  _onExampleLoad = (message) => {
    const { field, onExampleLoad } = this.props;

    const newExample = message.fields[field];

    onExampleLoad(newExample);
  };

  render() {
    const { example, field } = this.props;
    const originalMessage = <span id="xtrc-original-example" style={{ display: 'none' }}>{example}</span>;
    let messagePreview;

    if (example) {
      messagePreview = (
        <NewExampleWell bsSize="small">
          <span id="xtrc-example">{example}</span>
        </NewExampleWell>
      );
    } else {
      messagePreview = (
        <NoExample className="alert alert-warning">
          Could not load an example of field &lsquo;{field}&rsquo;. It is not possible to test
          the extractor before updating it.
        </NoExample>
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

export default ExtractorExampleMessage;
