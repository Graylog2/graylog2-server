import React, { PropTypes } from 'react';
import MessageLoader from './MessageLoader';

const ExtractorExampleMessage = React.createClass({
  propTypes: {
    field: PropTypes.string.isRequired,
    example: PropTypes.string,
    onExampleLoad: PropTypes.func,
  },
  _onExampleLoad(message) {
    const newExample = message.fields[this.props.field];
    this.props.onExampleLoad(newExample);
  },
  render() {
    const originalMessage = <span id="xtrc-original-example" style={{ display: 'none' }}>{this.props.example}</span>;
    let messagePreview;

    if (this.props.example) {
      messagePreview = (
        <div className="well well-sm xtrc-new-example">
          <span id="xtrc-example">{this.props.example}</span>
        </div>
      );
    } else {
      messagePreview = (
        <div className="alert alert-warning xtrc-no-example">
          Could not load an example of field '{this.props.field}'. It is not possible to test
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
  },
});

export default ExtractorExampleMessage;
