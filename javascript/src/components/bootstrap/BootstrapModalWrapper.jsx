import React, {Component, PropTypes} from 'react';
import {Modal} from 'react-bootstrap';

class BootstrapModalWrapper extends Component {
  static propTypes = {
    showModal: PropTypes.bool,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
    onOpen: PropTypes.func,
    onClose: PropTypes.func,
  };

  constructor(props) {
    super(props);

    this.open = this.open.bind(this);
    this.close = this.close.bind(this);

    this.state = {
      showModal: props.showModal || false,
    };
  }

  open() {
    this.setState({showModal: true}, this.onOpen);
  }

  onOpen() {
    if (this.props.onOpen === 'function') {
      this.props.onOpen();
    }
  }

  close() {
    this.setState({showModal: false}, this.onClose);
  }

  onClose() {
    if (this.props.onClose === 'function') {
      this.props.onClose();
    }
  }

  render() {
    return (
      <Modal show={this.state.showModal} onHide={this.close}>
        {this.props.children}
      </Modal>
    );
  }
}

export default BootstrapModalWrapper;
