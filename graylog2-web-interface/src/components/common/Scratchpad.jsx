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
import React, { useContext, useState, useEffect, useRef } from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';
import ClipboardJS from 'clipboard';

import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import { Alert, Button, MenuItem, SplitButton, OverlayTrigger, Tooltip } from 'components/graylog';
import { BootstrapModalConfirm } from 'components/bootstrap';
/* NOTE: common components are cyclical dependencies, so they need to be directly imported */
import InteractableModal from 'components/common/InteractableModal';
import Icon from 'components/common/Icon';
import Store from 'logic/local-storage/Store';

const DEFAULT_SCRATCHDATA = '';
const TEXTAREA_ID = 'scratchpad-text-content';

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const Textarea = styled.textarea(({ copied, theme }) => css`
  width: 100%;
  padding: 3px;
  resize: none;
  flex: 1;
  margin: 15px 0 7px;
  border: 1px solid ${copied ? theme.colors.variant.success : theme.colors.variant.lighter.default};
  box-shadow: inset 1px 1px 1px rgba(0, 0, 0, 0.075)${copied && `, 0 0 8px ${chroma(theme.colors.variant.success).alpha(0.4).css()}`};
  transition: border 150ms ease-in-out, box-shadow 150ms ease-in-out;
  font-family: ${theme.fonts.family.monospace};
  font-size: ${theme.fonts.size.body};

  :focus {
    border-color: ${theme.colors.variant.light.info};
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

const Footer = styled.footer(({ theme }) => css`
  display: flex;
  align-items: center;
  padding: 7px 0 9px;
  border-top: 1px solid ${theme.colors.gray[80]};
`);

const SavingMessage = styled.span(({ theme, visible }) => css`
  flex: 1;
  color: ${theme.colors.variant.success};
  font-style: italic;
  opacity: ${visible ? '1' : '0'};
  transition: opacity 150ms ease-in-out;
`);

const Scratchpad = () => {
  let clipboard;
  const textareaRef = useRef();
  const confirmationModalRef = useRef();
  const { isScratchpadVisible, setScratchpadVisibility, localStorageItem } = useContext(ScratchpadContext);
  const scratchpadStore = Store.get(localStorageItem) || {};
  const [isSecurityWarningConfirmed, setSecurityWarningConfirmed] = useState(scratchpadStore.securityConfirmed || false);
  const [scratchData, setScratchData] = useState(scratchpadStore.value || DEFAULT_SCRATCHDATA);
  const [size, setSize] = useState(scratchpadStore.size || undefined);
  const [copied, setCopied] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [recentlySaved, setRecentlySaved] = useState(false);
  const [position, setPosition] = useState(scratchpadStore.position || undefined);

  const writeData = (newData) => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, ...newData });
  };

  const handleSaveMessage = () => {
    if (dirty) {
      setRecentlySaved(true);

      setTimeout(() => {
        setRecentlySaved(false);
      }, 1500);
    }

    setDirty(false);
  };

  const handleChange = () => {
    const { value } = textareaRef.current;

    setDirty(true);
    setScratchData(value);
    writeData({ value });
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
        {!isSecurityWarningConfirmed && (
          <StyledAlert bsStyle="warning" bsSize="sm">
            <Icon name="exclamation-triangle" size="lg" />
            <AlertNote>We recommend you do <strong>not</strong> store any sensitive information, such as passwords, in this area.</AlertNote>
            <Button bsStyle="link" bsSize="sm" onClick={handleGotIt}>Got It!</Button>
          </StyledAlert>
        )}

        <Textarea ref={textareaRef}
                  onChange={handleChange}
                  onBlur={handleSaveMessage}
                  value={scratchData}
                  id={TEXTAREA_ID}
                  copied={copied}
                  spellCheck={false} />

        <Footer>
          <OverlayTrigger placement="right"
                          trigger={['hover', 'focus']}
                          overlay={(
                            <Tooltip id="scratchpad-help">
                              You can use this space to store personal notes and other information while interacting with
                              Graylog, without leaving your browser window. For example, store timestamps, user IDs, or IP
                              addresses you need in various investigations.
                            </Tooltip>
                          )}>
            <Button bsStyle="link">
              <Icon name="question-circle" />
            </Button>
          </OverlayTrigger>

          <SavingMessage visible={recentlySaved}><Icon name="hdd" type="regular" /> Saved!</SavingMessage>

          <SplitButton title={CopyWithIcon}
                       bsStyle="info"
                       data-clipboard-button
                       data-clipboard-target={`#${TEXTAREA_ID}`}
                       id="scratchpad-actions">
            <MenuItem onClick={openConfirmClear}><Icon name="trash-alt" /> Clear</MenuItem>
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
