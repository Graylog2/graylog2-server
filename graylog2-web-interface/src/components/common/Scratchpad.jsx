import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';

import isLocalStorageReady from 'util/isLocalStorageReady';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';

const ScratchpadBar = styled.div`
  width: ${({ opened }) => (opened ? '300px' : '30px')};
  overflow: hidden;
  box-shadow: -3px 0 3px ${({ opened }) => (opened ? 'rgba(0, 0, 0, .25)' : 'rgba(0, 0, 0, 0)')};
  transition: width 150ms ease-in-out, box-shadow 150ms ease-in-out;
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  height: 100%;
  background-color: #393939;
  display: flex;
  align-items: center;
`;

const ToggleButton = styled.button`
  width: 30px;
  height: 90vh;
  border: 0;
  padding: 0;
  background: transparent;
  color: #fff;
  display: flex;
  align-items: center;
  flex-direction: column;
  &::before,
  &::after {
    flex: 1;
    background: #393939;
    content: "";
    width: 5px;
    border: 1px solid #6C6C6C;
    border-top: 0;
    border-bottom: 0;
  }
  span {
    transform: rotate(90deg);
    display: block;
    white-space: nowrap;
    background-color: #393939;
    padding: 0 6px;
  }
`;

const ContentArea = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: calc(300px - 30px - 5px); /* Opened Width - Button Width - Right Padding */
  min-width: calc(300px - 30px - 5px); /* Opened Width - Button Width - Right Padding */
  padding-right: 5px;
`;

const Title = styled.h3`
  color: #fff;
  margin: 9px 0;
`;

const Description = styled.p`
  color: #fff;
`;

const Textarea = styled.textarea`
  width: 100%;
  resize: none;
  flex: 1;
`;

const StyledAlert = styled(Alert)`
  margin-bottom: 10px;
`;

const Scratchpad = () => {
  const [opened, setOpened] = useState(false);
  const [scratchData, setScratchData] = useState(localStorage.getItem(LOCALSTORAGE_ITEM));
  const [localStorageReady] = useState(isLocalStorageReady());
  const textareaRef = useRef();

  const toggleOpened = () => {
    setOpened(!opened);
  };

  const handleChange = () => {
    const { value } = textareaRef.current;
    setScratchData(value);

    if (localStorageReady) {
      localStorage.setItem(LOCALSTORAGE_ITEM, textareaRef.current.value);
    }
  };

  useEffect(() => {
    if (textareaRef.current && opened) {
      textareaRef.current.focus();
    }
  }, [opened]);

  return (
    <ScratchpadBar opened={opened}>
      <ToggleButton onClick={toggleOpened}>
        <span>{opened ? 'Close' : 'Open'} Scratchpad</span>
      </ToggleButton>

      <ContentArea>
        <Title>Scratchpad</Title>
        <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

        {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

        <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
      </ContentArea>
    </ScratchpadBar>
  );
};

export default Scratchpad;
