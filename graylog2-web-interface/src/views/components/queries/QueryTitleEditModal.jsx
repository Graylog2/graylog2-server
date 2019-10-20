import React from 'react';
import PropTypes from 'prop-types';

import { Modal, Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';

const QueryTitleEditModal = ({ show, toggleModal, onDraftChange, titleDraft, onSave }) => (
  <Modal show={show} bsSize="large" onHide={toggleModal}>
    <Modal.Header closeButton>
      <Modal.Title>Edit query title</Modal.Title>
    </Modal.Header>
    <Modal.Body>
      <Input autoFocus
             help="The title of the query tab."
             id="title"
             label="Title"
             name="title"
             onChange={onDraftChange}
             required
             type="text"
             value={titleDraft} />
    </Modal.Body>
    <Modal.Footer>
      <Button onClick={onSave} bsStyle="success">Save</Button>
      <Button onClick={toggleModal}>Cancel</Button>
    </Modal.Footer>
  </Modal>
);

QueryTitleEditModal.propTypes = {
  show: PropTypes.bool.isRequired,
  toggleModal: PropTypes.func.isRequired,
  onDraftChange: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
  titleDraft: PropTypes.func.isRequired,
};

export default QueryTitleEditModal;
