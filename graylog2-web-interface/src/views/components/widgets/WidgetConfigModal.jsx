import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { Modal, Button } from 'components/graylog';

const WidgetConfigModal = createReactClass({
  propTypes: {
    title: PropTypes.string.isRequired,
    show: PropTypes.bool,
    onSave: PropTypes.func.isRequired,
    onCancel: PropTypes.func,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node,
    ]),
  },

  getDefaultProps() {
    return {
      show: false,
      onCancel: () => {},
      children: [],
    };
  },

  getInitialState() {
    return {
      show: this.props.show,
    };
  },

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    if (this.props.show !== nextProps.show) {
      this.setState({ show: nextProps.show });
    }
  },

  handleSave() {
    this.handleClose();
    this.props.onSave();
  },

  handleClose() {
    this.setState({ show: false }, this.props.onCancel);
  },

  open() {
    this.setState({ show: true });
  },

  close() {
    this.handleClose();
  },

  render() {
    return (
      <Modal show={this.state.show} bsSize="large" onHide={this.handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>{this.props.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {this.props.children}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this.handleSave} bsStyle="success">Save</Button>
          <Button onClick={this.handleClose}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  },
});

export default WidgetConfigModal;
