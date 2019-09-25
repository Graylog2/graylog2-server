import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';
import { ResizableBox } from 'react-resizable';

import { Alert } from 'components/graylog';

import isLocalStorageReady from 'util/isLocalStorageReady';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';

const ScratchpadBar = styled(({ opened, ...props }) => <ResizableBox {...props} />)`
  width: ${({ opened }) => (opened ? '450px' : '30px')};
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
  order: 1;

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

const ScratchpadWrapper = styled.div`
  height: calc(100vh - 50px);
  position: relative;
  z-index: 2;
`;

const ContentArea = styled.div`
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 420px; /* 450 - 30 */
  order: 2;
  padding: 0 15px;
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
  margin-bottom: 15px;
`;

const StyledAlert = styled(Alert)`
  margin-bottom: 10px;
`;

const Scratchpad = () => {
  const [opened, setOpened] = useState(false);
  const [scratchPadHeight, setScratchPadHeight] = useState(0);
  const [scratchData, setScratchData] = useState(localStorage.getItem(LOCALSTORAGE_ITEM));
  const [localStorageReady] = useState(isLocalStorageReady());
  const scratchPadWrapperRef = useRef();
  const textareaRef = useRef();
  const toggleButtonRef = useRef();

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

  useEffect(() => {
    if (scratchPadWrapperRef.current) {
      setScratchPadHeight(scratchPadWrapperRef.current.offsetHeight);
    }
  }, [scratchPadWrapperRef.current]);

  return (
    <ScratchpadWrapper ref={scratchPadWrapperRef}>
      <ScratchpadBar opened={opened}
                     width={opened ? 450 : 30}
                     height={scratchPadHeight}
                     minConstraints={[450, scratchPadHeight]}
                     maxConstraints={[900, scratchPadHeight]}
                     axis={opened ? 'x' : 'none'}
                     handle={(
                       <ToggleButton onClick={toggleOpened} ref={toggleButtonRef}>
                         <span>{opened ? 'Close' : 'Open'} Scratchpad</span>
                       </ToggleButton>
    )}>
        <ContentArea>
          <Title>Scratchpad</Title>
          <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

          {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

          <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
        </ContentArea>
      </ScratchpadBar>
    </ScratchpadWrapper>
  );
};

export default Scratchpad;
