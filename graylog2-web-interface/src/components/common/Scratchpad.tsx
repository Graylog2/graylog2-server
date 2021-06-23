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
import styled, { css, DefaultTheme } from 'styled-components';
import chroma from 'chroma-js';
import ClipboardJS from 'clipboard';
import debounce from 'lodash/debounce';

import { ScratchpadContext } from 'contexts/ScratchpadProvider';
import { Alert, Button, ButtonGroup, OverlayTrigger, Tooltip } from 'components/graylog';
import { BootstrapModalConfirm } from 'components/bootstrap';
import InteractableModal from 'components/common/InteractableModal';
import Icon from 'components/common/Icon';
import Store from 'logic/local-storage/Store';

const DEFAULT_SCRATCHDATA = '';
const TEXTAREA_ID = 'scratchpad-text-content';
const STATUS_CLEARED = 'Cleared.';
const STATUS_COPIED = 'Copied!';
const STATUS_AUTOSAVED = 'Auto saved.';
const STATUS_DEFAULT = '';

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const Textarea = styled.textarea(({ $copied, theme }: {$copied: boolean, theme:DefaultTheme}) => css`
  width: 100%;
  padding: 3px;
  resize: none;
  flex: 1;
  margin: 15px 0 7px;
  border: 1px solid ${$copied ? theme.colors.variant.success : theme.colors.variant.lighter.default};
  box-shadow: inset 1px 1px 1px rgba(0, 0, 0, 0.075)${$copied && `, 0 0 8px ${chroma(theme.colors.variant.success).alpha(0.4).css()}`};
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
    margin: 6px 0 0;
    display: flex;
    align-items: center;
  }
`;

const AlertNote = styled.em`
  margin-left: 6px;
  flex: 1;
`;

const Footer = styled.footer(({ theme }: { theme: DefaultTheme }) => css`
  display: flex;
  align-items: center;
  padding: 7px 0 9px;
  border-top: 1px solid ${theme.colors.gray[80]};
`);

const StatusMessage = styled.span(({ theme, $visible }: { theme: DefaultTheme, $visible: boolean}) => css`
  flex: 1;
  color: ${theme.colors.variant.success};
  font-style: italic;
  opacity: ${$visible ? '1' : '0'};
  transition: opacity 150ms ease-in-out;
`);

const Scratchpad = () => {
  const clipboard = useRef<typeof ClipboardJS>();
  const textareaRef = useRef<HTMLTextAreaElement>();
  const confirmationModalRef = useRef<typeof BootstrapModalConfirm>();
  const statusTimeout = useRef<ReturnType<typeof setTimeout>>();
  const { isScratchpadVisible, setScratchpadVisibility, localStorageItem } = useContext(ScratchpadContext);
  const scratchpadStore = Store.get(localStorageItem) || {};
  const [isSecurityWarningConfirmed, setSecurityWarningConfirmed] = useState<boolean>(scratchpadStore.securityConfirmed || false);
  const [scratchData, setScratchData] = useState<string>(scratchpadStore.value || DEFAULT_SCRATCHDATA);
  const [size, setSize] = useState<{ width: string, height: string } | undefined>(scratchpadStore.size || undefined);
  const [position, setPosition] = useState<{ x:number, y:number } | undefined>(scratchpadStore.position || undefined);
  const [statusMessage, setStatusMessage] = useState<string>(STATUS_DEFAULT);
  const [showStatusMessage, setShowStatusMessage] = useState<boolean>(false);

  const writeData = (newData) => {
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

  const updateStatusMessage = (message) => {
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
    updateStatusMessage(STATUS_CLEARED);
  };

  const handleCancelClear = () => {
    confirmationModalRef.current.close();
  };

  useEffect(() => {
    clipboard.current = new ClipboardJS('[data-clipboard-button]', {});

    clipboard.current.on('success', () => {
      updateStatusMessage(STATUS_COPIED);
    });

    return () => {
      clipboard.current.destroy();

      if (statusTimeout.current) {
        clearTimeout(statusTimeout.current);
      }
    };
  });

  useEffect(() => {
    if (textareaRef.current && isScratchpadVisible) {
      textareaRef.current.focus();
      textareaRef.current.value = scratchData;
    }
  }, [scratchData, isScratchpadVisible]);

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
            <AlertNote>We recommend you do <strong>not</strong> store any sensitive information, such as passwords, in
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
                            <Tooltip id="scratchpad-help" show>
                              You can use this space to store personal notes and other information while interacting with
                              Graylog, without leaving your browser window. For example, store timestamps, user IDs, or IP
                              addresses you need in various investigations.
                            </Tooltip>
                          )}>
            <Button bsStyle="link">
              <Icon name="question-circle" />
            </Button>
          </OverlayTrigger>

          <StatusMessage $visible={showStatusMessage}>
            <Icon name={statusMessage === STATUS_COPIED ? 'copy' : 'hdd'} type="regular" /> {statusMessage}
          </StatusMessage>

          <ButtonGroup>
            <Button data-clipboard-button
                    data-clipboard-target={`#${TEXTAREA_ID}`}
                    id="scratchpad-actions"
                    title="Copy">
              <Icon name="copy" />
            </Button>
            <Button onClick={openConfirmClear} title="Clear">
              <Icon name="trash-alt" />
            </Button>
          </ButtonGroup>

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
