import * as React from 'react';
import { useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { Icon } from 'components/common';

const PlaceholderBox = styled.div(({ theme }) => css`
  opacity: 0;
  transition: visibility 0s, opacity 0.2s linear;
  
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;  
  text-align: center;
  padding: 10px;
  
  background-color: ${theme.colors.global.contentBackground};
  color: ${theme.colors.gray[30]};
  margin-bottom: ${theme.spacings.xs};
  
  :hover {
    opacity: 1;
  }
`);

const HugeIcon = styled(Icon)(({ theme }) => css`
  font-size: ${theme.fonts.size.huge};
  margin-bottom: 10px;
`);

type ChildProps = {
  onCancel: () => void,
  position: WidgetPosition,
}

type Props = {
  style?: React.CSSProperties,
  position: WidgetPosition,
  component: React.ComponentType<ChildProps>,
}

const NewWidgetPlaceholder = React.forwardRef<HTMLDivElement, Props>(({ style, position, component: Component }, ref) => {
  const [show, setShow] = useState(false);
  const onCancel = useCallback(() => setShow(false), []);
  const onClick = useCallback(() => setShow(true), []);

  const containerStyle = {
    ...style,
    transition: 'none',
  };

  return (
    <div style={containerStyle} ref={ref}>
      <PlaceholderBox onClick={onClick}>
        <HugeIcon name="circle-plus" />
        Create a new widget here
      </PlaceholderBox>
      {show && <Component onCancel={onCancel} position={position} />}
    </div>
  );
});

NewWidgetPlaceholder.defaultProps = {
  style: {},
};

export default NewWidgetPlaceholder;
