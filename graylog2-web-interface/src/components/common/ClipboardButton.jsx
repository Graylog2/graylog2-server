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
import ClipboardJS from 'clipboard';

import { Tooltip, OverlayTrigger, Button } from 'components/graylog';

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
    /** Container element which is focussed */
    container: PropTypes.any,
  };

  static defaultProps = {
    action: 'copy',
    disabled: false,
    buttonTitle: undefined,
    container: undefined,
    text: undefined,
    target: undefined,
    className: undefined,
    style: undefined,
    bsStyle: undefined,
    bsSize: undefined,
    onSuccess: () => {},
  };

  state = {
    tooltipMessage: '',
  };

  componentDidMount() {
    const { container } = this.props;
    const options = {};

    if (container) {
      options.container = container;
    }

    this.clipboard = new ClipboardJS('[data-clipboard-button]', options);
    this.clipboard.on('success', this._onSuccess);
    this.clipboard.on('error', this._onError);
  }

  componentWillUnmount() {
    if (this.clipboard) {
      this.clipboard.destroy();
    }
  }

  _onSuccess = (event) => {
    const { onSuccess } = this.props;

    this.setState({ tooltipMessage: 'Copied!' });

    onSuccess(event);

    event.clearSelection();
  };

  _onError = (event) => {
    const key = event.action === 'cut' ? 'K' : 'C';

    this.setState({ tooltipMessage: <span>Press Ctrl+{key}&thinsp;/&thinsp;&#8984;{key} to {event.action}</span> });
  };

  _getFilteredProps = () => {
    const { className, style, bsStyle, bsSize, disabled, buttonTitle } = this.props;

    return { className, style, bsStyle, bsSize, disabled, title: buttonTitle };
  };

  render() {
    const { action, title, text, target } = this.props;
    const { tooltipMessage } = this.state;

    const filteredProps = this._getFilteredProps();
    const tooltip = <Tooltip id="copy-button-tooltip">{tooltipMessage}</Tooltip>;

    if (text) {
      filteredProps['data-clipboard-text'] = text;
    } else {
      filteredProps['data-clipboard-target'] = target;
    }

    return (
      <OverlayTrigger placement="top" trigger="click" overlay={tooltip} rootClose>
        <Button data-clipboard-button data-clipboard-action={action} {...filteredProps}>
          {title}
        </Button>
      </OverlayTrigger>
    );
  }
}

export default ClipboardButton;
