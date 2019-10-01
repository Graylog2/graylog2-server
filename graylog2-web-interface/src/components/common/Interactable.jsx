import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import interact from 'interactjs';
import { merge } from 'lodash';
import styled from 'styled-components';

const Interactable = ({ className, resizable, draggable, draggableOptions, resizableOptions, children, width, height }) => {
  const boxRef = useRef();
  const [boxCoords, setBoxCoords] = useState({ x: 0, y: 0 });
  let interactable;

  const defaultDraggableOptions = {
    inertia: true,
    autoScroll: true,

    modifiers: [
      interact.modifiers.restrict({
        restriction: window.document.body,
        elementRect: { top: 0, left: 0, bottom: 1, right: 1 },
        endOnly: true,
      }),
    ],

    onmove: (event) => {
      // keep the dragged position in the data-x/data-y attributes
      const x = (parseFloat(boxRef.current.getAttribute('data-x')) || 0) + event.dx;
      const y = (parseFloat(boxRef.current.getAttribute('data-y')) || 0) + event.dy;

      // translate the element
      setBoxCoords({ x, y });
    },
  };

  const mergedDraggableOptions = merge(defaultDraggableOptions, draggableOptions);

  const setInteractions = () => {
    if (interactable) {
      if (draggable) interactable.draggable(mergedDraggableOptions);
      if (resizable) interactable.resizable(resizableOptions);
    }
  };

  useEffect(() => {
    if (boxRef.current) {
      interactable = interact(boxRef.current);
      setInteractions();
    }
  }, [boxRef.current]);

  return (
    <StyledInteractable ref={boxRef}
                        className={className}
                        width={width}
                        height={height}
                        data-x={boxCoords.x}
                        data-y={boxCoords.y}>
      {children}
    </StyledInteractable>
  );
};

const StyledInteractable = styled.div`
  touch-action: none;
  width: ${props => props.width};
  height: ${props => props.height};
  position: relative;
  z-index: 999;
  transform: translate(${props => props['data-x']}px, ${props => props['data-y']}px);
`;

Interactable.propTypes = {
  resizable: PropTypes.bool,
  draggable: PropTypes.bool,
  width: PropTypes.string.isRequired,
  height: PropTypes.string.isRequired,
  draggableOptions: PropTypes.object,
  resizableOptions: PropTypes.object,
  children: PropTypes.any.isRequired,
  className: PropTypes.string,
};

Interactable.defaultProps = {
  resizable: false,
  draggable: false,
  draggableOptions: {},
  resizableOptions: {},
  className: undefined,
};

export default Interactable;
