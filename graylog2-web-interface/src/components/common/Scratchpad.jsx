import React, { useContext, useState, useEffect, useRef } from 'react';
import styled, { css } from 'styled-components';
import { rgba } from 'polished';
import ClipboardJS from 'clipboard';

import teinte from 'theme/teinte';
import { Alert, Button, MenuItem, SplitButton } from 'components/graylog';
import { BootstrapModalConfirm } from 'components/bootstrap';
import { ScratchpadContext } from 'routing/context/ScratchpadProvider';
/* NOTE: common components are cyclical dependencies, so they need to be directly imported */
import InteractableModal from 'components/common/InteractableModal';
import Icon from 'components/common/Icon';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';
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

const Scratchpad = () => {
  let clipboard;
  const storage = JSON.parse(localStorage.getItem(LOCALSTORAGE_ITEM)) || {};
  const textareaRef = useRef();
  const confirmationModalRef = useRef();
  const { isScratchpadVisible, setScratchpadVisibility } = useContext(ScratchpadContext);
  const [isSecurityWarningConfirmed, setSecurityWarningConfirmed] = useState(storage.securityConfirmed || false);
  const [scratchData, setScratchData] = useState(storage.value || DEFAULT_SCRATCHDATA);
  const [size, setSize] = useState(storage.size || undefined);
  const [copied, setCopied] = useState(false);
  const [position, setPosition] = useState(storage.position || undefined);

  const writeData = (newData) => {
    const currentStorage = JSON.parse(localStorage.getItem(LOCALSTORAGE_ITEM));
    localStorage.setItem(LOCALSTORAGE_ITEM, JSON.stringify({ ...currentStorage, ...newData }));
  };

  const handleChange = () => {
    const { value } = textareaRef.current;

    setScratchData(value);
    writeData({ value: textareaRef.current.value });
  };

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
        <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

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

export default Scratchpad;
