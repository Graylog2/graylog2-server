import * as React from 'react';
import styled, { css } from 'styled-components';

import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import AddMessageCountActionHandler from 'views/logic/fieldactions/AddMessageCountActionHandler';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { Icon } from 'components/common';

const PlaceholderBox = styled.div(({ theme }) => css`
  opacity: 0;
  transition: visibility 0s, opacity 0.2s linear;
  
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;  
  
  background-color: ${theme.colors.global.contentBackground};
  border: 1px dashed ${theme.colors.variant.lighter.default};
  margin-bottom: ${theme.spacings.xs};
  border-radius: 4px;
  font-size: ${theme.fonts.size.huge};
  
  :hover {
    opacity: 1;
  }
`);

type Props = {
  style?: React.CSSProperties,
  position: WidgetPosition,
}

const NewWidgetPlaceholder = React.forwardRef<HTMLDivElement, Props>(({ style, position }, ref) => {
  const containerStyle = {
    ...style,
    transition: 'none',
  };

  const onClick = async () => {
    const newWidget = await AddMessageCountActionHandler({});

    return CurrentViewStateActions.updateWidgetPosition(newWidget.id, position);
  };

  return (
    <div style={containerStyle} ref={ref}>
      <PlaceholderBox onClick={onClick}>
        <Icon name="circle-plus" />
      </PlaceholderBox>
    </div>
  );
});

NewWidgetPlaceholder.defaultProps = {
  style: {},
};

export default NewWidgetPlaceholder;
