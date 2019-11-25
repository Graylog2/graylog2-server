import PropTypes from 'prop-types';
import React from 'react';

import { Well } from 'components/graylog';

import MessageLoader from './MessageLoader';

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
        <Well bsSize="small" className="xtrc-new-example">
          <span id="xtrc-example">{example}</span>
        </Well>
      );
    } else {
      messagePreview = (
        <div className="alert alert-warning xtrc-no-example">
          Could not load an example of field &lsquo;{field}&rsquo;. It is not possible to test
          the extractor before updating it.
        </div>
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
