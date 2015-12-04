import React, {PropTypes} from 'react';
import MessageLoader from './MessageLoader';

const ExtractorExampleMessage = React.createClass({
  propTypes: {
    field: PropTypes.string.isRequired,
    example: PropTypes.string,
  },
  getInitialState() {
    return {
      example: '',
      field: '',
      newMessageLoaded: false,
    };
  },
  componentWillMount() {
    this.setState({example: this.props.example});
  },
  componentWillReceiveProps(nextProps) {
    if (!this.state.newMessageLoaded && this.state.example !== nextProps.example) {
      this.setState({example: nextProps.example});
    }
  },
  onExampleLoaded(message) {
    const newExample = message.fields[this.props.field];

    if (newExample) {
      this.setState({example: newExample, newMessageLoaded: true});
    }
  },
  render() {
    const originalMessage = <span id="xtrc-original-example" style={{display: 'none'}}>{this.state.example}</span>;
    let messagePreview;

    if (this.state.example) {
      messagePreview = (
        <div className="well well-sm xtrc-new-example">
          <span id="xtrc-example">{this.state.example}</span>
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
        <MessageLoader onMessageLoaded={this.onExampleLoaded}/>
      </div>
    );
  },
});

export default ExtractorExampleMessage;
