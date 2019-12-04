import React, { useRef, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Rnd } from 'react-rnd';
import styled from 'styled-components';
import { lighten } from 'polished';

import { Button } from 'components/graylog';
import Icon from 'components/common/Icon';
import teinte from 'theme/teinte';

const DEFAULT_SIZE = { width: 450, height: 300 };
const halfWidth = Math.ceil((document.body.offsetWidth / 2) - (DEFAULT_SIZE.width / 2));
const halfHeight = Math.ceil((document.body.offsetHeight / 2) - (DEFAULT_SIZE.height / 2));
const stayOnScreenHeight = halfHeight < 0 ? 55 : halfHeight;
const DEFAULT_POSITION = {
  x: halfWidth,
  y: stayOnScreenHeight,
};

const InteractableWrapper = styled.div`
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
`;

/**
 * A resizable and draggable modal component
 *
 * Can be controlled or uncontrolled, using [`react-rnd`](https://github.com/bokuweb/react-rnd) under the hood
 */

const Interactable = ({
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

  return (
    <InteractableWrapper className={wrapperClassName}>
      <StyledRnd default={{ ...position, ...size }}
                 minHeight={minHeight}
                 minWidth={minWidth}
                 maxHeight={document.body.offsetHeight}
                 maxWidth={document.body.offsetWidth}
                 dragHandleClassName={dragHandleClassName}
                 onDragStop={(event, newPosition) => {
                   const setPosition = {
                     x: newPosition.x,
                     y: newPosition.y,
                   };
                   setDragPosition(setPosition);
                   onDrag(setPosition);
                 }}
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
    </InteractableWrapper>
  );
};

Interactable.propTypes = {
  /** className that will be applied to `react-rnd` */
  className: PropTypes.string,
  /** Content of the Interactable modal */
  children: PropTypes.node.isRequired,
  /** Minimum height that modal can be reduced to */
  minHeight: PropTypes.number,
  /** Minimum width that modal can be reduced to */
  minWidth: PropTypes.number,
  /** Function that is called when Interactable is closed */
  onClose: PropTypes.func,
  /** Function that is called when Interactable has finished being dragged */
  onDrag: PropTypes.func,
  /** Function that is called when Interactable has finished being resized */
  onResize: PropTypes.func,
  /** If you want to control Interactable you can pass specific position */
  position: PropTypes.shape({
    x: PropTypes.number,
    y: PropTypes.number,
  }),
  /** If you want to control Interactable you can pass specific size */
  size: PropTypes.shape({
    height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
  /** Title that appears at the top of the window */
  title: PropTypes.string,
  /** Used to style wrapping component */
  wrapperClassName: PropTypes.string,
};

Interactable.defaultProps = {
  className: undefined,
  minHeight: 250,
  minWidth: 250,
  onClose: () => {},
  onDrag: () => {},
  onResize: () => {},
  position: DEFAULT_POSITION,
  size: DEFAULT_SIZE,
  title: '',
  wrapperClassName: undefined,
};

export default Interactable;
