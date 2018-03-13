import PropTypes from 'prop-types';
import React from 'react';

import { BootstrapModalForm, Input } from 'components/bootstrap';
import { Button } from 'react-bootstrap';

class ContentPackUploadControls extends React.Component {

  constructor(props) {
    super(props);
    this._openModal = this._openModal.bind(this);
    this._save = this._save.bind(this);
  }

  _openModal() {
    this.uploadModal.open();
  }

  _closeModal() {
    this.uploadModal.close();
  }

  _save() {
    this._closeModal();
  }

  render() {
    return (
      <span>
        <Button id="upload-content-pack-button" bsStyle="info" bsSize="large" onClick={this._openModal}>Upload</Button>
        <BootstrapModalForm
          ref={(node) => { this.uploadModal = node; }}
          onSubmitForm={this._save}
          title="Upload Content Pack"
          submitButtonText="Upload">
          <Input
            id="upload-content-pack"
            label="Choose File"
            type="file"
            help="Choose Content Pack from disk" />
        </BootstrapModalForm>
      </span>
    );
  }
}

export default ContentPackUploadControls;
