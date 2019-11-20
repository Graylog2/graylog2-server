import React, { useContext, useState, useEffect, useRef } from 'react';
import styled from 'styled-components';
// import ClipboardJS from 'clipboard';

import teinte from 'theme/teinte';
import Interactable from 'components/common/Interactable';
import { Alert, Button, MenuItem, SplitButton } from 'components/graylog';
import { BootstrapModalConfirm } from 'components/bootstrap';
import Icon from 'components/common/Icon';
import { ScratchpadContext } from 'routing/context/ScratchpadProvider';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';
const DEFAULT_SCRATCHDATA = '';

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const Description = styled.p`
  color: ${teinte.primary.due};
  margin: 9px 0 6px;
`;

const Textarea = styled.textarea`
  width: 100%;
  resize: none;
  flex: 1;
  margin-bottom: 15px;
`;

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

const Scratchpad = () => {
  const storage = JSON.parse(localStorage.getItem(LOCALSTORAGE_ITEM)) || {};
  const textareaRef = useRef();
  const confirmationRef = useRef();
  const { isScratchpadVisible, setScratchpadVisibility } = useContext(ScratchpadContext);
  const [isSecurityWarningConfirmed, setSecurityWarningConfirmed] = useState(storage.securitryConfirmed || false);
  const [scratchData, setScratchData] = useState(storage.value || DEFAULT_SCRATCHDATA);
  const [size, setSize] = useState(storage.size || undefined);
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
    writeData({ securitryConfirmed: true });
  };

  const openConfirmClear = () => {
    confirmationRef.open();
  };

  const handleClearText = () => {
    writeData({ value: DEFAULT_SCRATCHDATA });
  };

  const handleCancelClear = () => {
    confirmationRef.close();
  };

  const CopyWithIcon = (<><Icon name="copy" /> Copy</>);

  useEffect(() => {
    if (textareaRef.current && isScratchpadVisible) {
      textareaRef.current.focus();
    }
  }, [isScratchpadVisible]);

  if (!isScratchpadVisible) return null;

  return (
    <Interactable title="Scratchpad"
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

        <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} id="scratchpad-text-content" />

        <SplitButton title={CopyWithIcon}
                     bsStyle="info"
                     data-clipboard-target="#scratchpad-text-content">
          <MenuItem bsStyle="danger" onClick={openConfirmClear}><Icon name="trash" /> Clear</MenuItem>
        </SplitButton>

        <BootstrapModalConfirm ref={confirmationRef}
                               title="Are you sure?"
                               onConfirm={handleClearText}
                               onCancel={handleCancelClear}>
           This will clear out your Scratchpad content, do you wish to proceed?
        </BootstrapModalConfirm>
      </ContentArea>

    </Interactable>
  );
};

export default Scratchpad;
