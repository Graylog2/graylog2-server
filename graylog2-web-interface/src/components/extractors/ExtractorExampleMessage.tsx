import React from 'react';
import styled, { css } from 'styled-components';

import { Well } from 'components/bootstrap';

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

type ExtractorExampleMessageProps = {
  field: string;
  example?: string;
  onExampleLoad?: (...args: any[]) => void;
};

class ExtractorExampleMessage extends React.Component<ExtractorExampleMessageProps, {
  [key: string]: any;
}> {
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
