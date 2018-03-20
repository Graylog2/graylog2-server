import React from 'react';

import UserNotification from 'util/UserNotification';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { Button } from 'react-bootstrap';
import CombinedProvider from 'injection/CombinedProvider';

const { ContentPacksActions } = CombinedProvider.get('ContentPacks');

class ContentPackUploadControls extends React.Component {
  constructor(props) {
    super(props);
    this._openModal = this._openModal.bind(this);
    this._closeModal = this._closeModal.bind(this);
    this._save = this._save.bind(this);
  }

  _openModal() {
    this.uploadModal.open();
  }

  _closeModal() {
    this.uploadModal.close();
  }

  _save(submitEvent) {
    submitEvent.preventDefault();
    if (!this.uploadInput.getInputDOMNode().files || !this.uploadInput.getInputDOMNode().files[0]) {
      return;
    }

    const reader = new FileReader();

    reader.onload = (evt) => {
      const request = evt.target.result;
      ContentPacksActions.create.triggerPromise(request)
        .then(
          () => {
            UserNotification.success('Content pack imported successfully', 'Success!');
            ContentPacksActions.list();
          },
          (response) => {
            const message = 'Error importing content pack, please ensure it is a valid JSON file. Check your ' +
              'Graylog logs for more information.';
            const title = 'Could not import content pack';
            let smallMessage = '';
            if (response.additional && response.additional.body && response.additional.body.message) {
              smallMessage = `<br /><small>${response.additional.body.message}</small>`;
            }
            UserNotification.error(message + smallMessage, title);
          });
    };

    reader.readAsText(this.uploadInput.getInputDOMNode().files[0]);
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
            ref={(node) => { this.uploadInput = node; }}
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
