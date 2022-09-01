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
import { useContext, useMemo } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { AdditionalContext } from 'views/logic/ActionContext';
import { useStore } from 'stores/connect';
import type { Stream } from 'views/stores/StreamsStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { Input } from 'components/messageloaders/Types';
import { MESSAGE_FIELD } from 'views/Constants';
import type MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import { InputsStore } from 'stores/inputs/InputsStore';

import CustomHighlighting from './CustomHighlighting';
import MessageDetail from './MessageDetail';
import DecoratedValue from './decoration/DecoratedValue';
import MessagePreview from './MessagePreview';
import type { Message } from './Types';

import TypeSpecificValue from '../TypeSpecificValue';
import HighlightMessageContext from '../contexts/HighlightMessageContext';

export const TableBody = styled.tbody<{ expanded?: boolean, highlighted?: boolean }>(({ expanded, highlighted, theme }) => `
  && {
    border-top: 0;

    ${expanded ? `
      border-left: 7px solid ${theme.colors.variant.light.info};
    ` : ''}

    ${highlighted ? `
      border-left: 7px solid ${theme.colors.variant.light.success};
    ` : ''}
  }
`);

const FieldsRow = styled.tr(({ theme }) => `
  cursor: pointer;

  td {
    min-width: 50px;
    word-break: break-word;
  }

  time {
    font-size: ${theme.fonts.size.body};
  }
`);

const MessageDetailRow = styled.tr`
  td {
    padding-top: 5px;
    border-top: 0;
  }

  .row {
    margin-right: 0;
  }

  div[class*="col-"] {
    padding-right: 0;
  }
`;

type Props = {
  config: MessagesWidgetConfig,
  disableSurroundingSearch?: boolean,
  expandAllRenderAsync: boolean,
  expanded: boolean,
  fields: FieldTypeMappingsList,
  message: Message,
  selectedFields?: Immutable.OrderedSet<string>,
  showMessageRow?: boolean,
  toggleDetail: (string) => void,
};

const isDecoratedField = (field, decorationStats) => decorationStats
  && (decorationStats.added_fields[field] !== undefined || decorationStats.changed_fields[field] !== undefined);

const fieldType = (fieldName, { decoration_stats: decorationStats }: { decoration_stats?: any }, fields) => (isDecoratedField(fieldName, decorationStats)
  ? FieldType.Decorated
  : ((fields && fields.find((f) => f.name === fieldName)) || { type: FieldType.Unknown }).type);

const _renderStrong = (children, strong = false) => {
  if (strong) {
    return <strong>{children}</strong>;
  }

  return children;
};

const MessageTableEntry = ({
  config,
  disableSurroundingSearch,
  expandAllRenderAsync,
  expanded,
  fields,
  message,
  showMessageRow,
  selectedFields = Immutable.OrderedSet<string>(),
  toggleDetail,
}: Props) => {
  const { inputs: inputsList = [] } = useStore(InputsStore);
  const { streams: streamsList = [] } = useStore(StreamsStore);
  const highlightMessageId = useContext(HighlightMessageContext);
  const additionalContextValue = useMemo(() => ({ message }), [message]);
  const allStreams = Immutable.List<Stream>(streamsList);
  const streams = Immutable.Map<string, Stream>(streamsList.map((stream) => [stream.id, stream]));
  const inputs = Immutable.Map<string, Input>(inputsList.map((input) => [input.id, input]));

  const _toggleDetail = () => {
    const isSelectingText = !!window.getSelection()?.toString();

    if (!isSelectingText) {
      toggleDetail(`${message.index}-${message.id}`);
    }
  };

  const colSpanFixup = selectedFields.size + 1;

  return (
    <AdditionalContext.Provider value={additionalContextValue}>
      <TableBody expanded={expanded} highlighted={message.id === highlightMessageId}>
        <FieldsRow onClick={_toggleDetail}>
          {selectedFields.toArray().map((selectedFieldName, idx) => {
            const type = fieldType(selectedFieldName, message, fields);

            return (
              <td key={selectedFieldName}>
                {_renderStrong(
                  <CustomHighlighting field={selectedFieldName} value={message.fields[selectedFieldName]}>
                    <TypeSpecificValue value={message.fields[selectedFieldName]}
                                       field={selectedFieldName}
                                       type={type}
                                       render={DecoratedValue} />
                  </CustomHighlighting>,
                  idx === 0,
                )}
              </td>
            );
          })}
        </FieldsRow>

        <MessagePreview showMessageRow={showMessageRow}
                        config={config}
                        colSpanFixup={colSpanFixup}
                        messageFieldType={fieldType(MESSAGE_FIELD, message, fields)}
                        onRowClick={_toggleDetail}
                        message={message} />

        {expanded && (
          <MessageDetailRow>
            <td colSpan={colSpanFixup}>
              <MessageDetail message={message}
                             fields={fields}
                             streams={streams}
                             allStreams={allStreams}
                             inputs={inputs}
                             disableSurroundingSearch={disableSurroundingSearch}
                             expandAllRenderAsync={expandAllRenderAsync} />
            </td>
          </MessageDetailRow>
        )}
      </TableBody>
    </AdditionalContext.Provider>
  );
};

MessageTableEntry.propTypes = {
  disableSurroundingSearch: PropTypes.bool,
  expandAllRenderAsync: PropTypes.bool.isRequired,
  expanded: PropTypes.bool.isRequired,
  fields: PropTypes.object.isRequired,
  message: PropTypes.shape({
    fields: PropTypes.object.isRequired,
    highlight_ranges: PropTypes.object,
    id: PropTypes.string.isRequired,
    index: PropTypes.string.isRequired,
    decoration_stats: PropTypes.shape({
      added_fields: PropTypes.object,
      changed_fields: PropTypes.object,
      removed_fields: PropTypes.object,
    }),
  }).isRequired,
  // @ts-ignore
  selectedFields: PropTypes.instanceOf(Immutable.OrderedSet),
  showMessageRow: PropTypes.bool,
  toggleDetail: PropTypes.func.isRequired,
};

MessageTableEntry.defaultProps = {
  disableSurroundingSearch: false,
  selectedFields: Immutable.OrderedSet(),
  showMessageRow: false,
};

export default MessageTableEntry;
