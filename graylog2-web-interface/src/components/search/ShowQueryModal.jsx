import React from 'react';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import { Modal } from 'react-bootstrap';

import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import { ClipboardButton } from 'components/common';

const ShowQueryModal = React.createClass({
  propTypes: {
    builtQuery: React.PropTypes.string,
  },

  mixins: [PureRenderMixin],

  open() {
    this.refs.modal.open();
  },

  close() {
    this.refs.modal.close();
  },

  render() {
    const queryText = JSON.stringify(JSON.parse(this.props.builtQuery), null, '  ');
    return (
      <BootstrapModalWrapper ref="modal">
        <Modal.Header closeButton>
          <Modal.Title>Elasticsearch Query</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <pre>{queryText}</pre>
        </Modal.Body>
        <Modal.Footer>
          <ClipboardButton title="Copy query" target=".modal-body pre" />
        </Modal.Footer>
      </BootstrapModalWrapper>
    );
  },
});

export default ShowQueryModal;
