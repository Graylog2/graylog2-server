import React, { useContext, useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { rgba } from 'polished';
import ClipboardJS from 'clipboard';
import { debounce } from 'lodash';

import teinte from 'theme/teinte';
import { Alert, Button, MenuItem, SplitButton } from 'components/graylog';
import { BootstrapModalConfirm } from 'components/bootstrap';
import { ScratchpadContext } from 'providers/ScratchpadProvider';
/* NOTE: common components are cyclical dependencies, so they need to be directly imported */
import InteractableModal from 'components/common/InteractableModal';
import Icon from 'components/common/Icon';
import Store from 'logic/local-storage/Store';
import UserNotification from 'util/UserNotification';

const LOCALSTORAGE_PREFIX = 'gl-scratchpad-';
const DEFAULT_SCRATCHDATA = '';
const TEXTAREA_ID = 'scratchpad-text-content';

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const Description = styled.p`
  color: ${teinte.primary.due};
  margin: 9px 0 6px;
`;

const Textarea = styled.textarea(props => css`
  width: 100%;
  resize: none;
  flex: 1;
  margin-bottom: 15px;
  border: 1px solid ${props.copied ? teinte.tertiary.tre : teinte.secondary.tre};
  box-shadow: inset 0 1px 1px rgba(0, 0, 0, .075),
              0 0 8px ${rgba(props.copied ? teinte.tertiary.tre : teinte.secondary.tre, 0.6)};
  transition: border 150ms ease-in-out, box-shadow 150ms ease-in-out;

  :focus {
    border-color: ${teinte.tertiary.due};
    outline: none;
  }
`);

const StyledAlert = styled(Alert)`
  && {
    padding: 6px 12px;
    margin-bottom: 9px;
    display: flex;
    align-items: center;
  }
`;

const AlertNote = styled.em`
  margin-left: 6px;
  flex: 1;
`;

const Footer = styled.footer`
  display: flex;
  justify-content: flex-end;
  padding-bottom: 9px;
`;

const Scratchpad = ({ loginName }) => {
  let clipboard;
  const localStorageItem = `${LOCALSTORAGE_PREFIX}${loginName}`;
  const scratchpadStore = Store.get(localStorageItem) || {};
  const textareaRef = useRef();
  const confirmationModalRef = useRef();
  const { isScratchpadVisible, setScratchpadVisibility } = useContext(ScratchpadContext);
  const [isSecurityWarningConfirmed, setSecurityWarningConfirmed] = useState(scratchpadStore.securityConfirmed || false);
  const [scratchData, setScratchData] = useState(scratchpadStore.value || DEFAULT_SCRATCHDATA);
  const [size, setSize] = useState(scratchpadStore.size || undefined);
  const [copied, setCopied] = useState(false);
  const [position, setPosition] = useState(scratchpadStore.position || undefined);

  const writeData = (newData) => {
    const currentStorage = Store.get(localStorageItem);
    Store.set(localStorageItem, { ...currentStorage, ...newData });
  };

  const handleChange = debounce(() => {
    const { value } = textareaRef.current;

    UserNotification.success('Scratchpad data successfully saved.', 'Auto-Saved');
    setScratchData(value);
    writeData({ value: textareaRef.current.value });
  }, 300);

  const handleDrag = (newPosition) => {
    setPosition(newPosition);
    writeData({ position: newPosition });
  };

  const handleSize = (newSize) => {
    setSize(newSize);
    writeData({ size: newSize });
  };

  const handleGotIt = () => {
    setSecurityWarningConfirmed(true);
    writeData({ securityConfirmed: true });
  };

  const openConfirmClear = () => {
    confirmationModalRef.current.open();
  };

  const handleClearText = () => {
    setScratchData(DEFAULT_SCRATCHDATA);
    writeData({ value: DEFAULT_SCRATCHDATA });
    confirmationModalRef.current.close();
  };

  const handleCancelClear = () => {
    confirmationModalRef.current.close();
  };

  const CopyWithIcon = <><Icon name="copy" /> Copy</>;

  useEffect(() => {
    if (textareaRef.current && isScratchpadVisible) {
      textareaRef.current.focus();
    }

    clipboard = new ClipboardJS('[data-clipboard-button]', {});
    clipboard.on('success', () => {
      setCopied(true);
      setTimeout(() => {
        setCopied(false);
      }, 1000);
    });

    return () => {
      clipboard.destroy();
    };
  }, [isScratchpadVisible]);

  if (!isScratchpadVisible) return null;

  return (
    <InteractableModal title="Scratchpad"
                       onClose={() => setScratchpadVisibility(false)}
                       onDrag={handleDrag}
                       onResize={handleSize}
                       size={size}
                       position={position}>
      <ContentArea>
        <Description>You can use this space to store personal notes and other information while interacting with Graylog, without leaving your browser window. For example, store timestamps, user IDs, or IP addresses you need in various investigations.</Description>

        {!isSecurityWarningConfirmed && (
          <StyledAlert bsStyle="warning" bsSize="sm">
            <Icon name="exclamation-triangle" size="lg" />
            <AlertNote>We recommend you do <strong>not</strong> store any sensitive information, such as passwords, in this area.</AlertNote>
            <Button bsStyle="link" bsSize="sm" onClick={handleGotIt}>Got It!</Button>
          </StyledAlert>
        )}

        <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} id={TEXTAREA_ID} copied={copied} />

        <Footer>
          <SplitButton title={CopyWithIcon}
                       bsStyle="info"
                       data-clipboard-button
                       data-clipboard-target={`#${TEXTAREA_ID}`}
                       id="scratchpad-actions">
            <MenuItem onClick={openConfirmClear}><Icon name="trash" /> Clear</MenuItem>
          </SplitButton>
        </Footer>

      </ContentArea>

      <BootstrapModalConfirm ref={confirmationModalRef}
                             title="Are you sure?"
                             onConfirm={handleClearText}
                             onCancel={handleCancelClear}>
           This will clear out your Scratchpad content, do you wish to proceed?
      </BootstrapModalConfirm>
    </InteractableModal>
  );
};

Scratchpad.propTypes = {
  loginName: PropTypes.string.isRequired,
};

export default Scratchpad;
