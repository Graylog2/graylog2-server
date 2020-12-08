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

import UserNotification from 'util/UserNotification';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { Button } from 'components/graylog';
import CombinedProvider from 'injection/CombinedProvider';

import style from './ContentPackUploadControls.css';

const { ContentPacksActions } = CombinedProvider.get('ContentPacks');

class ContentPackUploadControls extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      isOpen: false,
    };

    this._openModal = this._openModal.bind(this);
    this._closeModal = this._closeModal.bind(this);
    this._save = this._save.bind(this);
  }

  _openModal() {
    this.setState({ isOpen: true });
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
            const message = 'Error importing content pack, please ensure it is a valid JSON file. Check your '
              + 'Graylog logs for more information.';
            const title = 'Could not import content pack';
            let smallMessage = '';

            if (response.additional && response.additional.body && response.additional.body.message) {
              smallMessage = `<br /><small>${response.additional.body.message}</small>`;
            }

            UserNotification.error(message + smallMessage, title);
          },
        );
    };

    reader.readAsText(this.uploadInput.getInputDOMNode().files[0]);
    this._closeModal();
  }

  render() {
    const { isOpen } = this.state;

    return (
      <span>
        <Button className={style.button}
                active={isOpen}
                id="upload-content-pack-button"
                bsStyle="success"
                onClick={this._openModal}>Upload
        </Button>
        <BootstrapModalForm onModalClose={() => { this.setState({ isOpen: false }); }}
                            ref={(node) => { this.uploadModal = node; }}
                            onSubmitForm={this._save}
                            title="Upload Content Pack"
                            submitButtonText="Upload">
          <Input ref={(node) => { this.uploadInput = node; }}
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
