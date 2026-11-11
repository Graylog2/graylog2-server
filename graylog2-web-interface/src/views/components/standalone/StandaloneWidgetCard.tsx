/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import styled from 'styled-components';

import WidgetCard from 'views/components/widgets/WidgetCard';
import TimerangeInfo from 'views/components/widgets/TimerangeInfo';
import useWidget from 'views/hooks/useWidget';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

import StandaloneWidgetContent from './StandaloneWidgetContent';
import useWidgetTitle from './useWidgetTitle';

const Content = styled.div`
  flex: 1;
  min-height: 0;
`;

const Footer = styled.div`
  display: flex;
  justify-content: flex-end;
  font-size: ${({ theme }) => theme.fonts.size.small};
`;

type Props = {
  widgetId: string;
  actions?: React.ReactNode;
  className?: string;
};

/**
 * Shows a widget of the enclosing `StandaloneWidgetGroup` in a card with its title and time range,
 * like the widgets of a dashboard, without their controls.
 */
const StandaloneWidgetCard = ({ widgetId, actions = undefined, className = undefined }: Props) => {
  const widget = useWidget(widgetId);
  const title = useWidgetTitle(widget);
  const activeQuery = useActiveQueryId();

  return (
    <WidgetCard headline={title} actions={actions} className={className}>
      <Content>
        <StandaloneWidgetContent widgetId={widgetId} />
      </Content>
      <Footer>
        <TimerangeInfo widget={widget} activeQuery={activeQuery} widgetId={widgetId} />
      </Footer>
    </WidgetCard>
  );
};

export default StandaloneWidgetCard;
