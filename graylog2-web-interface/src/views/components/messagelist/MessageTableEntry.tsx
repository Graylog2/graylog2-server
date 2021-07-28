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
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { StreamsStore, Stream } from 'views/stores/StreamsStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import { MESSAGE_FIELD } from 'views/Constants';
import { Input } from 'components/messageloaders/Types';

import MessageDetail from './MessageDetail';
import DecoratedValue from './decoration/DecoratedValue';
import CustomHighlighting from './CustomHighlighting';
import type { Message } from './Types';
import MessageSummaryRow from './MessageSummaryRow';

import TypeSpecificValue from '../TypeSpecificValue';

const { InputsStore } = CombinedProvider.get('Inputs');

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

export const MessageRow = styled.tr(({ theme }) => `
  && {
    margin-bottom: 5px;
    cursor: pointer;
  
    td {
      border-top: 0;
      padding-top: 0;
      padding-bottom: 5px;
      font-family: ${theme.fonts.family.monospace};
      color: ${theme.colors.variant.dark.info};
    }
  }
`);

export const MessageWrapper = styled.div`
  line-height: 1.5em;
  white-space: pre-line;
  max-height: 6em; /* show 4 lines: line-height * 4 */
  overflow: hidden;
`;

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
  disableSurroundingSearch?: boolean,
  expandAllRenderAsync: boolean,
  expanded: boolean,
  fields: FieldTypeMappingsList,
  highlightMessage?: string,
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

const MessageTableEntry = ({
  disableSurroundingSearch,
  expandAllRenderAsync,
  expanded,
  fields,
  highlightMessage = '',
  message,
  showMessageRow = false,
  selectedFields = Immutable.OrderedSet<string>(),
  toggleDetail,
}: Props) => {
  const { inputs: inputsList = [] } = useStore(InputsStore);
  const { streams: streamsList = [] } = useStore(StreamsStore);
  const allStreams = Immutable.List<Stream>(streamsList);
  const streams = Immutable.Map<string, Stream>(streamsList.map((stream) => [stream.id, stream]));
  const inputs = Immutable.Map<string, Input>(inputsList.map((input) => [input.id, input]));

  const _toggleDetail = () => {
    toggleDetail(`${message.index}-${message.id}`);
  };

  const _renderStrong = (children, strong = false) => {
    if (strong) {
      return <strong>{children}</strong>;
    }

    return children;
  };

  const colSpanFixup = selectedFields.size + 1;

  return (
    <TableBody expanded={expanded} highlighted={message.id === highlightMessage}>
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

      {showMessageRow && (
        <MessageRow onClick={_toggleDetail}>
          <td colSpan={colSpanFixup}>
            <MessageWrapper>
              <CustomHighlighting field="message" value={message.fields[MESSAGE_FIELD]}>
                <TypeSpecificValue field="message" value={message.fields[MESSAGE_FIELD]} type={fieldType(MESSAGE_FIELD, message, fields)} render={DecoratedValue} />
              </CustomHighlighting>
            </MessageWrapper>
          </td>
        </MessageRow>
      )}

      <MessageSummaryRow onClick={_toggleDetail} colSpanFixup={colSpanFixup} message={message} />

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
  );
};

MessageTableEntry.propTypes = {
  disableSurroundingSearch: PropTypes.bool,
  expandAllRenderAsync: PropTypes.bool.isRequired,
  expanded: PropTypes.bool.isRequired,
  fields: PropTypes.object.isRequired,
  highlightMessage: PropTypes.string,
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
  highlightMessage: undefined,
  selectedFields: Immutable.OrderedSet(),
  showMessageRow: false,
};

export default MessageTableEntry;
