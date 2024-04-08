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
import debounce from 'lodash/debounce';

import { OverlayTrigger } from 'components/common';
import { Alert, Button, ButtonGroup, BootstrapModalConfirm } from 'components/bootstrap';
import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import InteractableModal from 'components/common/InteractableModal';
import Icon from 'components/common/Icon';
import Store from 'logic/local-storage/Store';
import useHotkey from 'hooks/useHotkey';
import copyToClipboard from 'util/copyToClipboard';

const DEFAULT_SCRATCHDATA = '';
const TEXTAREA_ID = 'scratchpad-text-content';
const STATUS_CLEARED = 'Cleared.';
const STATUS_COPIED = 'Copied!';
const STATUS_AUTOSAVED = 'Auto saved.';
const STATUS_DEFAULT = '';

type Position = { x: number, y: number };
type ScratchpadSize = { width: string, height: string }

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const Textarea = styled.textarea<{ $copied: boolean }>(({ $copied, theme }) => css`
  width: 100%;
  padding: 3px;
  resize: none;
  flex: 1;
  margin: 15px 0 7px;
  border: 1px solid ${$copied ? theme.colors.variant.success : theme.colors.variant.lighter.default};
  box-shadow: inset 1px 1px 1px rgb(0 0 0 / 7.5%)${$copied && `, 0 0 8px ${chroma(theme.colors.variant.success).alpha(0.4).css()}`};
  transition: border 150ms ease-in-out, box-shadow 150ms ease-in-out;
  font-family: ${theme.fonts.family.monospace};
  font-size: ${theme.fonts.size.body};

  &:focus {
    border-color: ${theme.colors.variant.light.info};
    outline: none;
  }
`);

const StyledAlert = styled(Alert)`
  && {
    padding: 6px 12px;
    margin: 6px 0 0;
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

const StatusMessage = styled.span<{ $visible: boolean }>(({ theme, $visible }) => css`
  flex: 1;
  color: ${theme.colors.variant.success};
  font-style: italic;
  opacity: ${$visible ? '1' : '0'};
  transition: opacity 150ms ease-in-out;
`);

const Scratchpad = () => {
  const textareaRef = useRef<HTMLTextAreaElement>();
  const statusTimeout = useRef<ReturnType<typeof setTimeout>>();
  const { setScratchpadVisibility, localStorageItem } = useContext(ScratchpadContext);
  const scratchpadStore = Store.get(localStorageItem) || {};
  const [isSecurityWarningConfirmed, setSecurityWarningConfirmed] = useState<boolean>(scratchpadStore.securityConfirmed || false);
  const [scratchData, setScratchData] = useState<string>(scratchpadStore.value || DEFAULT_SCRATCHDATA);
  const [size, setSize] = useState<ScratchpadSize | undefined>(scratchpadStore.size || undefined);
  const [position, setPosition] = useState<Position | undefined>(scratchpadStore.position || undefined);
  const [statusMessage, setStatusMessage] = useState<string>(STATUS_DEFAULT);
  const [showStatusMessage, setShowStatusMessage] = useState<boolean>(false);
  const [showModal, setShowModal] = useState<boolean>(false);

  const writeData = (newData: Record<string, unknown>) => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, ...newData });
  };

  const resetStatusTimer = () => {
    if (statusTimeout.current) {
      clearTimeout(statusTimeout.current);
    }

    statusTimeout.current = setTimeout(() => {
      setShowStatusMessage(false);
    }, 1000);
  };

  const updateStatusMessage = (message: string) => {
    setStatusMessage(message);
    setShowStatusMessage(true);
    resetStatusTimer();
  };

  const handleChange = debounce(() => {
    const { value } = textareaRef.current;

    setScratchData(value);
    updateStatusMessage(STATUS_AUTOSAVED);
    writeData({ value });
  }, 500);

  const handleDrag = (newPosition: Position) => {
    setPosition(newPosition);
    writeData({ position: newPosition });
  };

  const handleSize = (newSize: ScratchpadSize) => {
    setSize(newSize);
    writeData({ size: newSize });
  };

  const handleGotIt = () => {
    setSecurityWarningConfirmed(true);
    writeData({ securityConfirmed: true });
  };

  const openConfirmClear = () => {
    setShowModal(true);
  };

  const handleCancelClear = () => {
    setShowModal(false);
  };

  const handleClearText = () => {
    setScratchData(DEFAULT_SCRATCHDATA);
    writeData({ value: DEFAULT_SCRATCHDATA });
    handleCancelClear();
    updateStatusMessage(STATUS_CLEARED);
  };

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.focus();
      textareaRef.current.value = scratchData;
    }
  }, [scratchData]);

  useHotkey({
    actionKey: 'clear',
    scope: 'scratchpad',
    callback: openConfirmClear,
    options: { enableOnFormTags: true, preventDefault: true },
  });

  const copyCallback = () => {
    copyToClipboard(scratchData).then(() => updateStatusMessage(STATUS_COPIED));
  };

  useHotkey({
    actionKey: 'copy',
    scope: 'scratchpad',
    callback: copyCallback,
    options: { enableOnFormTags: true },
  });

  return (
    <InteractableModal title="Scratchpad"
                       onClose={() => setScratchpadVisibility(false)}
                       onDrag={handleDrag}
                       onResize={handleSize}
                       size={size}
                       position={position}>
      <ContentArea>
        {!isSecurityWarningConfirmed && (
          <StyledAlert bsStyle="warning">
            <AlertNote>
              We recommend you do <strong>not</strong> store any sensitive information, such as passwords, in
              this area.
            </AlertNote>
            <Button bsStyle="link" bsSize="sm" onClick={handleGotIt}>Got It!</Button>
          </StyledAlert>
        )}

        <Textarea ref={textareaRef}
                  onChange={handleChange}
                  id={TEXTAREA_ID}
                  $copied={statusMessage === STATUS_COPIED}
                  spellCheck={false} />

        <Footer>
          <OverlayTrigger placement="right"
                          trigger={['hover', 'focus']}
                          overlay={(
                            <>
                              You can use this space to store personal notes and other information while interacting with
                              Graylog, without leaving your browser window. For example, store timestamps, user IDs, or IP
                              addresses you need in various investigations.
                            </>
                          )}>
            <Button bsStyle="link">
              <Icon name="help" />
            </Button>
          </OverlayTrigger>

          <StatusMessage $visible={showStatusMessage}>
            <Icon name={statusMessage === STATUS_COPIED ? 'content_copy' : 'save'} type="regular" /> {statusMessage}
          </StatusMessage>

          <ButtonGroup>
            <Button id="scratchpad-actions" onClick={copyCallback} title="Copy">
              <Icon name="content_copy" />
            </Button>
            <Button onClick={openConfirmClear} title="Clear">
              <Icon name="delete" />
            </Button>
          </ButtonGroup>

        </Footer>

      </ContentArea>

      <BootstrapModalConfirm showModal={showModal}
                             title="Are you sure?"
                             onConfirm={handleClearText}
                             onCancel={handleCancelClear}>
        This will clear out your Scratchpad content, do you wish to proceed?
      </BootstrapModalConfirm>
    </InteractableModal>
  );
};

export default Scratchpad;
