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
