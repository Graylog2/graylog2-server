import PropTypes from 'prop-types';
import React from 'react';
import { Button, Tooltip, OverlayTrigger } from 'react-bootstrap';
import Clipboard from 'clipboard';

/**
 * Component that renders a button to copy some text in the clipboard when pressed.
 * The text to be copied can be given in the `text` prop, or in an external element through a CSS selector in the `target` prop.
 */
const ClipboardButton = React.createClass({
  propTypes: {
    /** Text or element used in the button. */
    title: PropTypes.oneOfType([PropTypes.node, PropTypes.string]).isRequired,
    /** Action to perform. */
    action: PropTypes.oneOf(['copy', 'cut']),
    /** Text to be copied in the clipboard. This overrides the `target` prop. */
    text: PropTypes.string,
    /** CSS selector to an element containing the text to be copied to the clipboard. This will only be used if `text` is not provided. */
    target: PropTypes.string,
    /** Function to call if text was successfully copied to clipboard. */
    onSuccess: PropTypes.func,
    /** Button's class name. */
    className: PropTypes.string,
    /** Button's style. */
    style: PropTypes.object,
    /** Button's bsStyle. */
    bsStyle: PropTypes.string,
    /** Button's bsSize. */
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
