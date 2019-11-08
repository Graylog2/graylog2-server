import React from 'react';
import PropTypes from 'prop-types';
import { Rnd } from 'react-rnd';
import styled from 'styled-components';
import { lighten } from 'polished';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';
import teinte from 'theme/teinte';

import 'react-mops/dist/esm/index.css';

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
  z-index: 9999;
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
  margin: 0 0 9px;
  padding: 6px 12px;
  display: flex;
  align-items: center;
  background-color: ${lighten(0.25, teinte.primary.tre)};
  border-bottom: 1px solid ${teinte.primary.tre};
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
`;

const Title = styled.h3`
  color: ${teinte.primary.due};
  flex: 1;
  cursor: move;
`;

const Interactable = ({
  children,
  className,
  onClose,
  onDrag,
  onResize,
  position,
  size,
  title,
}) => {
  const dragHandleRef = React.useRef(null);
  const [dragHandleClassName, setDragHandleClassName] = React.useState(null);
  const [dragPosition, setDragPosition] = React.useState(position);
  const [resizeSize, setResizeSize] = React.useState(size);

  React.useEffect(() => {
    setDragHandleClassName(dragHandleRef.current.classList[0]);
  }, []);

  return (
    <InteractableWrapper>
      <StyledRnd default={{ ...position, ...size }}
                 minHeight={250}
                 minWidth={250}
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
        <Header>
          <Title ref={dragHandleRef}>{title}</Title>

          {onClose
          && (
            <Button bsStyle="default" onClick={onClose} bsSize="sm">
              <Icon name="times" size="lg" />
            </Button>
          )}
        </Header>

        <Content>
          {children}
        </Content>
      </StyledRnd>
    </InteractableWrapper>
  );
};

Interactable.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
  onClose: PropTypes.func,
  onDrag: PropTypes.func,
  onResize: PropTypes.func,
  position: PropTypes.shape({
    x: PropTypes.number,
    y: PropTypes.number,
  }),
  size: PropTypes.shape({
    height: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    width: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
  title: PropTypes.string,
};

Interactable.defaultProps = {
  className: undefined,
  onClose: undefined,
  onDrag: () => {},
  onResize: () => {},
  position: DEFAULT_POSITION,
  size: DEFAULT_SIZE,
  title: '',
};

export default Interactable;
