import React, { useState, useEffect, useRef } from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import Interactable from 'components/common/Interactable';

import isLocalStorageReady from 'util/isLocalStorageReady';

const LOCALSTORAGE_ITEM = 'gl-scratchpad';

const ScratchpadBar = styled.div`
  width: ${({ opened }) => (opened ? '450px' : '450px')};
  overflow: hidden;
  box-shadow: -3px 0 3px ${({ opened }) => (opened ? 'rgba(0, 0, 0, .25)' : 'rgba(0, 0, 0, 0)')};
  transition: width 150ms ease-in-out, box-shadow 150ms ease-in-out;
  height: 25vh;
  background-color: #393939;
  display: flex;
  align-items: center;
`;

const ScratchpadWrapper = styled.div`
  height: calc(100vh - 50px);
  position: relative;
  z-index: 11;
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
    <Interactable draggable width={opened ? '450px' : '450px'} height={`${scratchPadHeight}px`}>
      <ScratchpadWrapper ref={scratchPadWrapperRef}>
        <ScratchpadBar opened={opened}
                       width={opened ? 450 : 30}
                       height={scratchPadHeight}
                       minConstraints={[450, scratchPadHeight]}
                       maxConstraints={[900, scratchPadHeight]}
                       axis={opened ? 'x' : 'none'}>
          <ContentArea>
            <Title>Scratchpad</Title>
            <Description>Accusamus atque iste natus officiis laudantium mollitia numquam voluptatibus voluptates! Eligendi, totam dignissimos ipsum obcaecati corrupti qui omnis quibusdam fuga consequatur suscipit!</Description>

            {!localStorageReady && (<StyledAlert bsStyle="warning">Your browser does not appear to support localStorage, so your Scratchpad may not properly restore between page changes and refreshes.</StyledAlert>)}

            <Textarea ref={textareaRef} onChange={handleChange} value={scratchData} />
          </ContentArea>
        </ScratchpadBar>
      </ScratchpadWrapper>
    </Interactable>
  );
};

export default Scratchpad;
