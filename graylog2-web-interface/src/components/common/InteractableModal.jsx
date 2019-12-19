import React, { useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Rnd } from 'react-rnd';
import styled from 'styled-components';
import { lighten } from 'polished';

import { Button } from 'components/graylog';
import Icon from 'components/common/Icon';
import teinte from 'theme/teinte';
import { debounce } from 'lodash';

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

const StyledRnd = styled(Rnd)`
  box-shadow: 0 0 9px rgba(31, 31, 31, .25),
              0 0 6px rgba(31, 31, 31, .25),
              0 0 3px rgba(31, 31, 31, .25);
  background-color: ${lighten(0.15, teinte.primary.tre)};
  border-radius: 3px;
  flex-direction: column;
  display: flex !important;
  pointer-events: auto;
`;

const Content = styled.div`
  flex: 1;
  padding: 0 15px;
`;

const Header = styled.header`
  padding: 6px 12px 9px;
  display: flex;
  align-items: center;
  background-color: ${lighten(0.25, teinte.primary.tre)};
  border-bottom: 1px solid ${teinte.primary.tre};
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
  cursor: move;
`;

const Title = styled.h3`
  color: ${teinte.primary.due};
  flex: 1;
`;

const DragBars = styled(Icon)`
  color: ${teinte.secondary.tre};
  margin-right: 9px;
`;

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

  const handleDragStop = (event, newPosition) => {
    const { x, y } = newPosition;

    const setPosition = { x, y };
    setDragPosition(setPosition);
    onDrag(setPosition);
  };

  const handleBrowserResize = debounce(() => {
    const { x, y } = dragPosition;
    const { width, height } = resizeSize;
    const { innerWidth, innerHeight } = window;
    const boundingBox = {
      top: 0,
      right: innerWidth - width,
      bottom: innerHeight - height,
      left: 0,
    };

    const tooFarLeft = x < boundingBox.left;
    const tooFarRight = x > boundingBox.right;
    const newRight = tooFarRight ? boundingBox.right : x;
    const nextX = tooFarLeft ? 0 : newRight;

    const tooFarUp = y < boundingBox.top;
    const tooFarDown = y > boundingBox.bottom;
    const newDown = tooFarDown ? boundingBox.bottom : y;
    const nextY = tooFarUp ? 0 : newDown;

    handleDragStop(null, { x: nextX, y: nextY });
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
                 className={className}
                 bounds="window">
        <Header ref={dragHandleRef}>
          <Title><DragBars name="bars" />{title}</Title>

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
