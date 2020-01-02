import React, { useEffect } from 'react';
import PropTypes from 'prop-types';

import styled from 'styled-components';
import ClipboardJS from 'clipboard';
import { readableColor } from 'polished';

import { OverlayTrigger, Tooltip } from 'components/graylog';

const Name = styled.div`
  flex: 1;
  font-weight: bold;
`;

const Value = styled.div`
  text-align: right;
  opacity: 0.5;
  transition: opacity 150ms ease-in-out;
`;

const Swatch = styled.div(({ color }) => `
  height: 60px;
  background-color: ${color};
  border: 1px solid #222;
  color: ${readableColor(color)};
  display: flex;
  padding: 3px;
  flex-direction: column;
  cursor: pointer;
  margin-right: 6px;
  flex: 1;

  &:last-of-type {
    margin: 0;
  }

  &:hover {
    ${Value} {
      opacity: 1;
    }
  }
`);

const ColorSwatch = ({ color, name, path }) => {
  let clipboard;

  if (ClipboardJS.isSupported()) {
    useEffect(() => {
      clipboard = new ClipboardJS('[data-clipboard-button]', {});

      return () => {
        clipboard.destroy();
      };
    }, []);
  }

  return (
    <OverlayTrigger placement="top"
                    overlay={<Tooltip id={`tooltip-swatch-${path}`}>Copied!</Tooltip>}
                    trigger="click"
                    delayShow={300}
                    delayHide={150}
                    id={`overlay-swatch-${path}`}>
      <Swatch color={color}
              data-clipboard-button
              data-clipboard-text={path}>
        <Name>{name}</Name>
        <Value>{color}</Value>
      </Swatch>
    </OverlayTrigger>
  );
};

ColorSwatch.propTypes = {
  color: PropTypes.string.isRequired,
  name: PropTypes.string,
  path: PropTypes.string,
};

ColorSwatch.defaultProps = {
  name: '',
  path: '',
};

export default ColorSwatch;
