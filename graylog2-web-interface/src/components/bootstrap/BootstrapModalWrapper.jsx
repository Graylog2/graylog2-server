import React, { Component, PropTypes } from 'react';
import { Modal } from 'react-bootstrap';

/**
 * Encapsulates a react-bootstrap modal, hiding the state handling for the modal
 */
class BootstrapModalWrapper extends Component {
  constructor(props) {
    super(props);

    this.open = this.open.bind(this);
    this.close = this.close.bind(this);
    this._hide = this._hide.bind(this);

    this.state = {
      showModal: props.showModal || false,
    };
  }

  onOpen() {
    if (typeof this.props.onOpen === 'function') {
      this.props.onOpen();
    }
  }

  onClose() {
    if (typeof this.props.onClose === 'function') {
      this.props.onClose();
    }
  }

  onHide() {
    if (typeof this.props.onHide === 'function') {
      this.props.onHide();
    }
  }

  open() {
    this.setState({ showModal: true }, this.onOpen);
  }

  close() {
    this.setState({ showModal: false }, this.onClose);
  }

  _hide() {
    this.setState({ showModal: false }, this.onHide);
  }

  render() {
    return (
      <Modal show={this.state.showModal} onHide={this._hide}>
        {this.props.children}
      </Modal>
    );
  }
}

BootstrapModalWrapper.propTypes = {
  showModal: PropTypes.bool,
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
  onOpen: PropTypes.func,
  onClose: PropTypes.func,
  onHide: PropTypes.func,
};

export default BootstrapModalWrapper;
