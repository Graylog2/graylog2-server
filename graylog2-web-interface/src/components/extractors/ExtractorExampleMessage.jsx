import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Well } from 'components/graylog';

import MessageLoader from './MessageLoader';

const NewExampleWell = styled(Well)`
  margin-bottom: 5px;
  font-family: monospace;
  font-size: 14px;
  white-space: pre-wrap;
  word-wrap: break-word;
`;

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
