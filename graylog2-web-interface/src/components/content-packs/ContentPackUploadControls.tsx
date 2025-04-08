import React, { useState, useRef } from 'react';

import UserNotification from 'util/UserNotification';
import { BootstrapModalForm, Input, Button } from 'components/bootstrap';
import { ContentPacksActions } from 'stores/content-packs/ContentPacksStore';
import useProductName from 'customization/useProductName';

import style from './ContentPackUploadControls.css';

const ContentPackUploadControls = () => {
  const productName = useProductName();
  const [isOpen, setIsOpen] = useState(false);
  const uploadInputRef = useRef(null);

  const openModal = () => setIsOpen(true);
  const closeModal = () => setIsOpen(false);

  const save = (submitEvent) => {
    submitEvent.preventDefault();

    const uploadInput = uploadInputRef.current.getInputDOMNode();
    if (!uploadInput.files || !uploadInput.files[0]) {
      return;
    }

    const reader = new FileReader();

    reader.onload = (evt) => {
      const request = evt.target.result;

      ContentPacksActions.create.triggerPromise(request).then(
        () => {
          UserNotification.success('Content pack imported successfully', 'Success!');
          ContentPacksActions.list();
        },
        (response) => {
          const message = `Error importing content pack, please ensure it is a valid JSON file. Check your ${productName} server logs for more information.`;
          const title = 'Could not import content pack';
          let smallMessage = '';

          if (response.additional && response.additional.body && response.additional.body.message) {
            smallMessage = `<br /><small>${response.additional.body.message}</small>`;
          }

          UserNotification.error(message + smallMessage, title);
        },
      );
    };

    reader.readAsText(uploadInput.files[0]);
    closeModal();
  };

  return (
    <span>
      <Button
        className={style.button}
        active={isOpen}
        id="upload-content-pack-button"
        bsStyle="info"
        onClick={openModal}>
        Upload
      </Button>
      <BootstrapModalForm
        onCancel={closeModal}
        show={isOpen}
        onSubmitForm={save}
        title="Upload Content Pack"
        submitButtonText="Upload">
        <Input
          ref={uploadInputRef}
          id="upload-content-pack"
          label="Choose File"
          type="file"
          help="Choose Content Pack from disk"
        />
      </BootstrapModalForm>
    </span>
  );
};

export default ContentPackUploadControls;
