import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import interact from 'interactjs'; // https://interactjs.io/docs/
import { debounce, merge } from 'lodash';
import styled from 'styled-components';

const StyledInteractable = styled.div.attrs(props => ({
  style: {
    width: `${props.width}px`,
    height: `${props.height}px`,
    top: `${props['data-y']}px`,
    left: `${props['data-x']}px`,
  },
}))`
  touch-action: none;
  position: absolute;
  z-index: 1001;
`;

const Interactable = ({
  children,
  className,
  draggable,
  draggableOptions,
  height,
  maxHeight,
  maxWidth,
  minHeight,
  minWidth,
  resizable,
  resizableOptions,
  width,
}) => {
  const boxRef = useRef();
  const [boxCoords, setBoxCoords] = useState({ x: 5, y: 55 });
  const [boxDimensions, setBoxDimensions] = useState({ width, height });
  let interactable;

  const parseDelta = (attr, delta = 0) => {
    return (parseFloat(boxRef.current.getAttribute(attr) || 0)) + delta;
  };

  const defaultDraggableOptions = {
    autoScroll: true,

    modifiers: [
      interact.modifiers.restrict({
        restriction: 'html > body',
        elementRect: { top: 0, left: 0, bottom: 1, right: 1 },
        endOnly: true,
      }),
    ],

    onmove: debounce((event) => {
      // keep the dragged position in the data-x/data-y attributes
      const x = parseDelta('data-x', event.dx);
      const y = parseDelta('data-y', event.dy);

      // translate the element
      setBoxCoords({ x, y });
    }, 2),
  };

  const defaultResizableOptions = {
    // resize from all edges and corners
    edges: { left: true, right: true, bottom: true, top: true },

    modifiers: [
      // keep the edges inside the parent
      interact.modifiers.restrictEdges({
        outer: 'parent',
        endOnly: true,
      }),

      // minimum/maximum sizes
      interact.modifiers.restrictSize({
        min: { width: minWidth, height: minHeight },
        max: { width: maxWidth, height: maxHeight },
      }),
    ],
  };

  const mergedDraggableOptions = merge(defaultDraggableOptions, draggableOptions);
  const mergedResizableOptions = merge(defaultResizableOptions, resizableOptions);

  const setInteractions = () => {
    if (interactable) {
      if (draggable) {
        interactable.draggable(mergedDraggableOptions);
      }

      if (resizable) {
        interactable.resizable(mergedResizableOptions).on('resizemove', debounce((event) => {
          // translate when resizing from top or left edges
          const x = parseDelta('data-x', event.deltaRect.left);
          const y = parseDelta('data-y', event.deltaRect.top);

          // update the element's style
          setBoxDimensions({ width: event.rect.width, height: event.rect.height });
          setBoxCoords({ x, y });
        }), 2);
      }
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
                        width={boxDimensions.width}
                        height={boxDimensions.height}
                        data-x={boxCoords.x}
                        data-y={boxCoords.y}>
      {children}
    </StyledInteractable>
  );
};

Interactable.propTypes = {
  resizable: PropTypes.bool,
  draggable: PropTypes.bool,
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  draggableOptions: PropTypes.object,
  resizableOptions: PropTypes.object,
  children: PropTypes.any.isRequired,
  className: PropTypes.string,
  minWidth: PropTypes.number,
  minHeight: PropTypes.number,
  maxWidth: PropTypes.number,
  maxHeight: PropTypes.number,
};

Interactable.defaultProps = {
  resizable: false,
  draggable: false,
  draggableOptions: {},
  resizableOptions: {},
  className: undefined,
  minWidth: 100,
  minHeight: 100,
  maxWidth: 900,
  maxHeight: 500,
};

export default Interactable;
