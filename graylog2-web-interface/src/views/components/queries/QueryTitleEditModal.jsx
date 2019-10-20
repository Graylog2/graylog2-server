import React from 'react';
import PropTypes from 'prop-types';

import { Modal, Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';

class QueryTitleEditModal extends React.Component {
  state = {
    show: false,
    titleDraft: '',
  };

  open = (activeQueryTitle) => {
    this.setState({
      show: true,
      titleDraft: activeQueryTitle,
    });
  }

  close = () => {
    this.setState({
      show: false,
      titleDraft: '',
    });
  }

  _onDraftSave = () => {
    const { titleDraft } = this.state;
    const { onTitleChange, selectedQueryId } = this.props;
    onTitleChange(selectedQueryId, titleDraft);
    this.close();
  };

  // eslint-disable-next-line no-undef
  _onDraftChange = (evt: SyntheticInputEvent<HTMLInputElement>) => {
    this.setState({ titleDraft: evt.target.value });
  };

  render() {
    const { show, titleDraft } = this.state;
    return (
      <Modal show={show} bsSize="large" onHide={this.toggleModal}>
        <Modal.Header closeButton>
          <Modal.Title>Edit query title</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Input autoFocus
                 help="The title of the query tab."
                 id="title"
                 label="Title"
                 name="title"
                 onChange={this._onDraftChange}
                 required
                 type="text"
                 value={titleDraft} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this._onDraftSave} bsStyle="success">Save</Button>
          <Button onClick={this.toggleModal}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}

QueryTitleEditModal.propTypes = {
  onTitleChange: PropTypes.func.isRequired,
  selectedQueryId: PropTypes.number.isRequired,
};

export default QueryTitleEditModal;
