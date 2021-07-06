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

import { Modal } from 'components/graylog';

/**
 * Encapsulates a react-bootstrap modal, hiding the state handling for the modal
 */
class BootstrapModalWrapper extends React.Component {
  static propTypes = {
    showModal: PropTypes.bool,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
    onOpen: PropTypes.func,
    onClose: PropTypes.func,
    onHide: PropTypes.func,
    bsSize: PropTypes.oneOf([
      'large', 'lg', 'small', 'sm',
    ]),
    backdrop: PropTypes.oneOf(['static', true, false]),
    role: PropTypes.string,
  };

  static defaultProps = {
    showModal: false,
    onOpen: () => {},
    onClose: () => {},
    onHide: () => {},
    bsSize: undefined,
    backdrop: 'static',
    role: 'dialog',
  };

  constructor(props) {
    super(props);

    this.state = {
      showModal: props.showModal || false,
    };
  }

  open = () => {
    const { onOpen } = this.props;

    this.setState({ showModal: true }, onOpen);
  };

  close = () => {
    const { onClose } = this.props;

    this.setState({ showModal: false }, onClose);
  };

  hide = () => {
    const { onHide } = this.props;

    this.setState({ showModal: false }, onHide);
  };

  render() {
    const { showModal } = this.state;
    const { children, bsSize, backdrop, role } = this.props;

    return (
      <Modal show={showModal}
             onHide={this.hide}
             bsSize={bsSize}
             backdrop={backdrop}
             role={role}>
        {children}
      </Modal>
    );
  }
}

export default BootstrapModalWrapper;
