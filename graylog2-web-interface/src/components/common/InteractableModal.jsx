import React, { useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Rnd } from 'react-rnd';
import styled, { css } from 'styled-components';
import { lighten } from 'polished';

import { Button } from 'components/graylog';
import Icon from 'components/common/Icon';

const DEFAULT_SIZE = { width: 450, height: 300 };
const halfWidth = Math.ceil((document.body.offsetWidth / 2) - (DEFAULT_SIZE.width / 2));
const halfHeight = Math.ceil((document.body.offsetHeight / 2) - (DEFAULT_SIZE.height / 2));
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
  box-shadow: 0 0 9px rgba(31, 31, 31, .25),
              0 0 6px rgba(31, 31, 31, .25),
              0 0 3px rgba(31, 31, 31, .25);
  background-color: ${theme.color.gray[20]};
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
  background-color: ${theme.color.gray[30]};
  border-bottom: 1px solid ${theme.color.gray[0]};
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
  cursor: move;
`);

const Title = styled.h3(({ theme }) => css`
  color: ${theme.color.global.textAlt};
  flex: 1;
`);

const DragBars = styled(Icon)(({ theme }) => css`
  color: ${theme.color.gray[70]};
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

  useEffect(() => {
    setDragHandleClassName(dragHandleRef.current.classList[0]);
  }, []);

  const handleDragStop = (event, newPosition) => {
    const { x, y, node } = newPosition;
    const width = parseFloat(node.style.width);
    const height = parseFloat(node.style.height);
    const bodyWidth = document.body.offsetWidth;
    const bodyHeight = document.body.offsetHeight;
    const boundingBox = {
      top: 0,
      right: bodyWidth - width,
      bottom: bodyHeight - height,
      left: 0,
    };

    const tooFarLeft = x < boundingBox.left;
    const tooFarRight = x > boundingBox.right;
    const newRight = tooFarRight ? bodyWidth - width : x;
    const newX = tooFarLeft ? 0 : newRight;

    const tooFarUp = y < boundingBox.top;
    const tooFarDown = y > boundingBox.bottom;
    const newDown = tooFarDown ? bodyHeight - height : y;
    const newY = tooFarUp ? 0 : newDown;

    const setPosition = {
      x: newX,
      y: newY,
    };
    setDragPosition(setPosition);
    onDrag(setPosition);
  };

  return (
    <InteractableModalWrapper className={wrapperClassName}>
      <StyledRnd default={{ ...position, ...size }}
                 minHeight={minHeight}
                 minWidth={minWidth}
                 maxHeight={document.body.offsetHeight}
                 maxWidth={document.body.offsetWidth}
                 dragHandleClassName={dragHandleClassName}
                 onDragStop={handleDragStop}
                 onResizeStop={(event, direction, ref) => {
                   const newSize = {
                     width: ref.style.width,
                     height: ref.style.height,
                   };
                   setResizeSize(newSize);
                   onResize(newSize);
                 }}
                 position={dragPosition}
                 size={resizeSize}
                 className={className}>
        <Header ref={dragHandleRef}>
          <Title><DragBars name="bars" />{' '}{title}</Title>

          <Button bsStyle="default" onClick={onClose} bsSize="sm">
            <Icon name="times" size="lg" />
          </Button>
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
