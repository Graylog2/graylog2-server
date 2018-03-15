import PropTypes from 'prop-types';
import React from 'react';
import { Button, Tooltip, OverlayTrigger } from 'react-bootstrap';
import Clipboard from 'clipboard';

/**
 * Component that renders a button to copy some text in the clipboard when pressed.
 * The text to be copied can be given in the `text` prop, or in an external element through a CSS selector in the `target` prop.
 */
class ClipboardButton extends React.Component {
  static propTypes = {
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
    /** Specifies if the button is disabled or not. */
    disabled: PropTypes.bool,
    /** Text to display when hovering over the button. */
    buttonTitle: PropTypes.string,
  };

  static defaultProps = {
    action: 'copy',
    disabled: false,
    buttonTitle: undefined,
  };

  state = {
    tooltipMessage: '',
  };

  componentDidMount() {
    this.clipboard = new Clipboard('[data-clipboard-button]');
    this.clipboard.on('success', this._onSuccess);
    this.clipboard.on('error', this._onError);
  }

  componentWillUnmount() {
    if (this.clipboard) {
      this.clipboard.destroy();
    }
  }

  _onSuccess = (event) => {
    this.setState({ tooltipMessage: 'Copied!' });

    if (this.props.onSuccess) {
      this.props.onSuccess(event);
    }

    event.clearSelection();
  };

  _onError = (event) => {
    const key = event.action === 'cut' ? 'K' : 'C';
    this.setState({ tooltipMessage: <span>Press Ctrl+{key}&thinsp;/&thinsp;&#8984;{key} to {event.action}</span> });
  };

  _getFilteredProps = () => {
    const { className, style, bsStyle, bsSize, disabled, buttonTitle } = this.props;
    return {
      className: className,
      style: style,
      bsStyle: bsStyle,
      bsSize: bsSize,
      disabled: disabled,
      title: buttonTitle,
    };
  };

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
  }
}

export default ClipboardButton;
