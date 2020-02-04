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
  flex: 1;

  &:hover {
    ${Value} {
      opacity: 1;
    }
  }
`);


const Wrapped = ({ copyText, children }) => {
  if (!copyText) {
    return <>{children}</>;
  }

  return (
    <OverlayTrigger placement="top"
                    overlay={<Tooltip id={`tooltip-swatch-${copyText}`}>Copied!</Tooltip>}
                    trigger="click"
                    delayShow={300}
                    delayHide={150}>
      {children}
    </OverlayTrigger>
  );
};

const ColorSwatch = ({ className, color, name, copyText }) => {
  let clipboard;

  useEffect(() => {
    if (ClipboardJS.isSupported() && !!copyText) {
      clipboard = new ClipboardJS('[data-clipboard-button]', {});
    }

    return () => {
      if (clipboard) {
        clipboard.destroy();
      }
    };
  }, []);

  return (
    <Wrapped copyText={copyText}>
      <Swatch color={color}
              className={className}
              data-clipboard-button
              data-clipboard-text={copyText}>
        <Name>{name}</Name>
        <Value>{color}</Value>
      </Swatch>
    </Wrapped>
  );
};

ColorSwatch.propTypes = {
  className: PropTypes.string,
  color: PropTypes.string.isRequired,
  copyText: PropTypes.string,
  name: PropTypes.string,
};

ColorSwatch.defaultProps = {
  className: undefined,
  copyText: undefined,
  name: '',
};

Wrapped.propTypes = {
  children: PropTypes.node.isRequired,
  copyText: PropTypes.string.isRequired,
};

export default ColorSwatch;
