import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import { Button, Tooltip, OverlayTrigger } from 'react-bootstrap';
import Clipboard from 'clipboard';

const ClipboardButton = React.createClass({
  propTypes: {
    title: PropTypes.oneOfType([PropTypes.node, PropTypes.string]).isRequired,
    action: PropTypes.oneOf(['copy', 'cut']),
    text: PropTypes.string, // text to copy to clipboard
    target: PropTypes.string, // css selector for the target element
    onSuccess: PropTypes.func,
    className: PropTypes.string,
    style: PropTypes.object,
    bsStyle: PropTypes.string,
    bsSize: PropTypes.string,
  },
  getDefaultProps() {
    return {
      action: 'copy',
    };
  },
  getInitialState() {
    return {
      tooltipMessage: '',
    };
  },
  componentDidMount() {
    this.clipboard = new Clipboard('[data-clipboard-button]');
    this.clipboard.on('success', this._onSuccess);
    this.clipboard.on('error', this._onError);
  },
  componentWillUnmount() {
    if (this.clipboard) {
      this.clipboard.destroy();
    }
  },
  _onSuccess(event) {
    this.setState({ tooltipMessage: 'Copied!' });

    if (this.props.onSuccess) {
      this.props.onSuccess(event);
    }

    event.clearSelection();
  },
  _onError(event) {
    const key = event.action === 'cut' ? 'K' : 'C';
    this.setState({ tooltipMessage: `Press Ctrl+${key} to ${event.action}` });
  },
  _getFilteredProps() {
    const { className, style, bsStyle, bsSize } = this.props;
    return {
      className: className,
      style: style,
      bsStyle: bsStyle,
      bsSize: bsSize,
    };
  },
  render() {
    const filteredProps = this._getFilteredProps();
    const tooltip = <Tooltip id={'copy-button-tooltip'}>{this.state.tooltipMessage}</Tooltip>;

    if (this.props.text) {
      filteredProps['data-clipboard-text'] = this.props.text;
    } else {
      filteredProps['data-clipboard-target'] = this.props.target;
    }

    return (
      <OverlayTrigger placement="top" trigger="click" overlay={tooltip} rootClose>
        <Button data-clipboard-button data-clipboard-action={this.props.action} {...filteredProps}>
          {this.props.title}
        </Button>
      </OverlayTrigger>
    );
  },
});

export default ClipboardButton;
