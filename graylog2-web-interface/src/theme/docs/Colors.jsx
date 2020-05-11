import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ClipboardJS from 'clipboard';

import { Tooltip } from 'components/graylog';

const Name = styled.span`
  flex: 1;
  font-weight: bold;
  text-align: left;
`;

const Value = styled.span`
  text-align: right;
  opacity: 0.5;
  transition: opacity 150ms ease-in-out;
`;

const StyledTooltip = styled(Tooltip).attrs((props) => ({
  className: props.opened ? 'in' : '',
}))(({ opened }) => `
  display: ${opened ? 'block' : 'none'}
`);

const Wrapped = styled.div`
  flex: 1;
  position: relative;
`;

const Swatch = styled.button(({ color, theme }) => `
  height: 60px;
  background-color: ${color};
  border: 1px solid #222;
  color: ${theme.util.readableColor(color)};
  padding: 3px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
  padding: 3px 6px;
  width: 100%;

  &:hover {
    ${Value} {
      opacity: 1;
    }
  }
`);

const ColorSwatch = ({ className, color, name, copyText }) => {
  const [opened, setOpened] = useState(false);
  const swatchRef = useRef();
  let clipboard;

  useEffect(() => {
    if (ClipboardJS.isSupported() && !!copyText) {
      clipboard = new ClipboardJS(swatchRef.current, {});

      clipboard.on('success', () => {
        setOpened(true);
        setTimeout(() => {
          setOpened(false);
        }, 1000);
      });
    }

    return () => {
      if (clipboard) {
        clipboard.destroy();
      }
    };
  }, []);

  return (
    <Wrapped className={className}>
      <StyledTooltip placement="top"
                     opened={opened}
                     positionTop={-32}
                     id={`${copyText ? copyText.replace(/\./g, '-') : name}-tooltip`}>
        Copied!
      </StyledTooltip>

      <Swatch color={color}
              data-clipboard-button
              data-clipboard-text={copyText}
              ref={swatchRef}>
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

export default ColorSwatch;
