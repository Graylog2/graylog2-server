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
import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { Rnd } from 'react-rnd';
import styled, { css } from 'styled-components';
import debounce from 'lodash/debounce';

import { Button } from 'components/graylog';
import Icon from 'components/common/Icon';

const DEFAULT_SIZE = { width: 450, height: 400 };
const halfWidth = Math.ceil((window.innerWidth / 2) - (DEFAULT_SIZE.width / 2));
const halfHeight = Math.ceil((window.innerHeight / 2) - (DEFAULT_SIZE.height / 2));
const stayOnScreenHeight = halfHeight < 0 ? 55 : halfHeight;
const DEFAULT_POSITION = {
  x: halfWidth,
  y: stayOnScreenHeight,
};

const InteractableModalWrapper = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1030;
  pointer-events: none;
`;

const StyledRnd = styled(Rnd)(({ theme }) => css`
  box-shadow: 0 0 9px ${theme.colors.global.navigationBoxShadow},
    0 0 6px ${theme.colors.global.navigationBoxShadow},
    0 0 3px ${theme.colors.global.navigationBoxShadow};
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.variant.lightest.default};
  border-radius: 3px;
  flex-direction: column;
  display: flex !important;
  pointer-events: auto;
`);

const Content = styled.div`
  flex: 1;
  padding: 0 15px;
`;

const Header = styled.header(({ theme }) => css`
  padding: 6px 12px 9px;
  display: flex;
  align-items: center;
  background-color: ${theme.colors.variant.lightest.default};
  border-bottom: 1px solid ${theme.colors.variant.lighter.default};
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
  cursor: move;
`);

const Title = styled.h3(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  flex: 1;
`);

const DragBars = styled(Icon)(({ theme }) => css`
  color: ${theme.colors.variant.darker.default};
  margin-right: 9px;
`);

const CloseButton = styled(Button)(({ theme }) => css`
  && {
    color: ${theme.colors.variant.light.default};
    
    :hover {
      color: ${theme.colors.variant.default};
    }
  }
`);

/**
 * A resizable and draggable modal component
 *
 * Can be controlled or uncontrolled, using [`react-rnd`](https://github.com/bokuweb/react-rnd) under the hood
 */

const InteractableModal = ({
  children,
  className,
  minHeight,
  minWidth,
  onClose,
  onDrag,
  onResize,
  position,
  size,
  title,
  wrapperClassName,
}) => {
  const dragHandleRef = useRef(null);
  const [dragHandleClassName, setDragHandleClassName] = useState(null);
  const [dragPosition, setDragPosition] = useState(position);
  const [resizeSize, setResizeSize] = useState(size);

  const handleDragStop = (event, { x, y }) => {
    setDragPosition({ x, y });
    onDrag({ x, y });
  };

  const handleResizeStop = (event, direction, ref) => {
    const newSize = {
      width: ref.style.width,
      height: ref.style.height,
    };
    let newCoords = { ...dragPosition };

    switch (direction) {
      case 'left':
      case 'topLeft':
      case 'top':
        newCoords = {
          x: dragPosition.x - (parseFloat(ref.style.width) - parseFloat(resizeSize.width)),
          y: dragPosition.y - (parseFloat(ref.style.height) - parseFloat(resizeSize.height)),
        };

        break;

      case 'bottomLeft':
        newCoords = {
          x: dragPosition.x - (parseFloat(ref.style.width) - parseFloat(resizeSize.width)),
          y: dragPosition.y,
        };

        break;

      case 'topRight':
        newCoords = {
          x: dragPosition.x,
          y: dragPosition.y - (parseFloat(ref.style.height) - parseFloat(resizeSize.height)),
        };

        break;

      default:
        break;
    }

    setResizeSize(newSize);
    onResize(newSize);
    handleDragStop(null, newCoords);
  };

  const handleBrowserResize = debounce(() => {
    const { x: currentX, y: currentY } = dragPosition;
    const { width, height } = resizeSize;
    const { innerWidth, innerHeight } = window;

    const boundingBox = {
      top: 0,
      bottom: parseFloat(height),
      left: 0,
      right: parseFloat(width),
    };

    const newCoords = {};
    const modalXWithNewWidth = innerWidth - boundingBox.right;
    const modalYWithNewHeight = innerHeight - boundingBox.bottom;

    newCoords.x = Math.max(Math.min(modalXWithNewWidth, currentX), boundingBox.left);
    newCoords.y = Math.max(Math.min(modalYWithNewHeight, currentY), boundingBox.top);

    handleDragStop(null, newCoords);
  }, 150);

  useEffect(() => {
    setDragHandleClassName(dragHandleRef.current.classList[0]);
  }, []);

  useEffect(() => {
    window.addEventListener('resize', handleBrowserResize, false);

    return () => {
      window.removeEventListener('resize', handleBrowserResize);
    };
  }, [dragPosition]);

  return (
    <InteractableModalWrapper className={wrapperClassName}>
      <StyledRnd default={{ ...position, ...size }}
                 minHeight={minHeight}
                 minWidth={minWidth}
                 maxHeight={window.innerHeight}
                 maxWidth={window.innerWidth}
                 dragHandleClassName={dragHandleClassName}
                 onDragStop={handleDragStop}
                 onResizeStop={handleResizeStop}
                 position={dragPosition}
                 size={resizeSize}
                 className={className}
                 bounds="window">
        <Header ref={dragHandleRef}>
          <Title><DragBars name="bars" />{title}</Title>

          <CloseButton bsStyle="link" onClick={onClose} bsSize="small">
            <Icon name="times" size="lg" />
          </CloseButton>
        </Header>

        <Content>
          {children}
        </Content>
      </StyledRnd>
    </InteractableModalWrapper>
  );
};

InteractableModal.propTypes = {
  /** className that will be applied to `react-rnd` */
  className: PropTypes.string,
  /** Content of the InteractableModal modal */
  children: PropTypes.node.isRequired,
  /** Minimum height that modal can be reduced to */
  minHeight: PropTypes.number,
  /** Minimum width that modal can be reduced to */
  minWidth: PropTypes.number,
  /** Function that is called when InteractableModal is closed */
  onClose: PropTypes.func,
  /** Function that is called when InteractableModal has finished being dragged */
  onDrag: PropTypes.func,
  /** Function that is called when InteractableModal has finished being resized */
  onResize: PropTypes.func,
  /** If you want to control InteractableModal you can pass specific position */
  position: PropTypes.shape({
    x: PropTypes.number,
    y: PropTypes.number,
  }),
  /** If you want to control InteractableModal you can pass specific size */
  size: PropTypes.shape({
    height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
  /** Title that appears at the top of the window */
  title: PropTypes.string,
  /** Used to style wrapping component */
  wrapperClassName: PropTypes.string,
};

InteractableModal.defaultProps = {
  className: undefined,
  minHeight: DEFAULT_SIZE.height,
  minWidth: DEFAULT_SIZE.width,
  onClose: () => {},
  onDrag: () => {},
  onResize: () => {},
  position: DEFAULT_POSITION,
  size: DEFAULT_SIZE,
  title: '',
  wrapperClassName: undefined,
};

export default InteractableModal;
