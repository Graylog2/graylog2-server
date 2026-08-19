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
import { useState, useCallback } from 'react';
import styled, { css } from 'styled-components';

import MessageDetail from 'components/common/message/details/MessageDetail';
import MessageDetailProviders from 'components/common/message/details/MessageDetailProviders';
import WindowDimensionsContextProvider from 'contexts/WindowDimensionsContextProvider';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import type { BackendMessage, Message } from 'views/components/messagelist/Types';

type DetailState = {
  backendMessage: BackendMessage;
  laneLabel: string;
};

const Panel = styled.div(
  ({ theme }) => css`
    flex: 0 0 50%;
    display: flex;
    flex-direction: column;
    border-left: 1px solid ${theme.colors.input.border};
    background: ${theme.colors.global.contentBackground};
    overflow: hidden;
  `,
);

const PanelHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px ${theme.spacings.sm};
    border-bottom: 1px solid ${theme.colors.input.border};
    flex-shrink: 0;
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
    gap: 8px;
  `,
);

const PanelTitle = styled.span`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const CloseButton = styled.button(
  ({ theme }) => css`
    background: none;
    border: none;
    cursor: pointer;
    color: ${theme.colors.text.secondary};
    font-size: 1.2em;
    line-height: 1;
    padding: 0 2px;
    flex-shrink: 0;

    &:hover {
      color: ${theme.colors.text.primary};
    }
  `,
);

const PanelBody = styled.div`
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
`;

const formatTs = (isoString: string): string => isoString.replace('T', ' ').slice(0, 19);

const toMessage = (bm: BackendMessage): Message => ({
  id: bm.message._id as string,
  index: bm.index,
  fields: bm.message as Record<string, any>,
  formatted_fields: MessageFieldsFilter.filterFields(bm.message),
  highlight_ranges: bm.highlight_ranges,
  decoration_stats: bm.decoration_stats,
});

const useSwimlaneDetailDrawer = () => {
  const [detail, setDetail] = useState<DetailState | null>(null);

  const openDetail = useCallback((backendMessage: BackendMessage, laneLabel: string) => {
    setDetail({ backendMessage, laneLabel });
  }, []);

  const closeDetail = useCallback(() => setDetail(null), []);

  const detailPanel = detail ? (() => {
    const message = toMessage(detail.backendMessage);
    const timestamp = formatTs(detail.backendMessage.message.timestamp as string);
    const title = `${detail.laneLabel} · ${timestamp}`;

    return (
      <Panel>
        <PanelHeader>
          <PanelTitle title={title}>{title}</PanelTitle>
          <CloseButton type="button" onClick={closeDetail} aria-label="Close detail">×</CloseButton>
        </PanelHeader>
        <PanelBody id="sticky-augmentations-container">
          <div id={`sticky-augmentations-boundary-${message.id}`}>
            <WindowDimensionsContextProvider>
              <MessageDetailProviders message={message}>
                <MessageDetail
                  message={message}
                  disableSurroundingSearch
                  disableTestAgainstStream
                />
              </MessageDetailProviders>
            </WindowDimensionsContextProvider>
          </div>
        </PanelBody>
      </Panel>
    );
  })() : null;

  return { openDetail, detailPanel, isOpen: !!detail };
};

export default useSwimlaneDetailDrawer;
