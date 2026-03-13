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
import { useCallback, useContext, useMemo } from 'react';
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
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { TableDataCell } from 'views/components/datatable';
import MessageDetail from 'components/common/message/details/MessageDetail';
import DecoratedValue from 'views/components/messagelist/decoration/DecoratedValue';
import type { BackendMessage, Message } from 'views/components/messagelist/Types';
import CustomHighlighting from 'views/components/highlighting/CustomHighlighting';
import TypeSpecificValue from 'views/components/TypeSpecificValue';
import HighlightMessageContext from 'views/components/contexts/HighlightMessageContext';
import { toSelectableMessageTableEntry } from 'views/components/widgets/MessageTableSelectedEntitiesProvider';
import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import BulkSelectCell from 'components/common/message/messagetable/BulkSelectCell';

import MessagePreview from './MessagePreview';

export const TableBody = styled.tbody<{ $expanded?: boolean; $highlighted?: boolean }>(
  ({ $expanded, $highlighted, theme }) => `
  && {
    border-top: 0;

    ${
      $expanded
        ? `
  border-left: 7px solid ${theme.colors.variant.light.info};
`
        : ''
    }

    ${
      $highlighted
        ? `
  border-left: 7px solid ${theme.colors.variant.light.success};
`
        : ''
    }
  }
`,
);

const FieldsRow = styled.tr`
  cursor: pointer;

  && td:not([data-bulk-select-cell]) {
    min-width: 50px;
    word-break: break-word;
    padding: 4px 5px 2px;
  }

  time {
    line-height: 1;
  }
`;

const MessageDetailRow = styled.tr`
  td {
    padding-top: 5px;
    border-top: 0;
  }

  .row {
    margin-right: 0;
  }

  div[class*='col-'] {
    padding-right: 0;
  }
`;

type Props = {
  config: MessagesWidgetConfig;
  disableSurroundingSearch?: boolean;
  expandAllRenderAsync: boolean;
  expanded: boolean;
  fields: FieldTypeMappingsList;
  message: Message;
  selectedFields?: Immutable.OrderedSet<string>;
  showMessageRow?: boolean;
  toggleDetail: (messageId: string) => void;
  displayBulkSelectCol?: boolean;
  isEntitySelectable?: (entity: BackendMessage) => boolean;
};

const isDecoratedField = (field: string | number, decorationStats: Message['decoration_stats']) =>
  decorationStats &&
  (decorationStats.added_fields[field] !== undefined || decorationStats.changed_fields[field] !== undefined);

const fieldType = (fieldName: string, { decoration_stats: decorationStats }: Message, fields: FieldTypeMappingsList) =>
  isDecoratedField(fieldName, decorationStats)
    ? FieldType.Decorated
    : (fields?.find((f) => f.name === fieldName) ?? { type: FieldType.Unknown }).type;

const Strong = ({ children = undefined, strong }: React.PropsWithChildren<{ strong: boolean }>) =>
  strong ? <strong>{children}</strong> : <>{children}</>;

const MessageTableEntry = ({
  config,
  disableSurroundingSearch = false,
  expandAllRenderAsync,
  expanded,
  fields,
  message,
  showMessageRow = false,
  selectedFields = Immutable.OrderedSet<string>(),
  toggleDetail,
  displayBulkSelectCol = false,
  isEntitySelectable = () => false,
}: Props) => {
  const { inputs: inputsList = [] } = useStore(InputsStore);
  const { streams: streamsList = [] } = useStore(StreamsStore);
  const highlightMessageId = useContext(HighlightMessageContext);
  const { toggleEntitySelect, selectedEntities } = useSelectedEntities();
  const sendTelemetry = useSendTelemetry();
  const additionalContextValue = useMemo(() => ({ message }), [message]);
  const allStreams = useMemo(() => Immutable.List<Stream>(streamsList), [streamsList]);
  const streams = useMemo(
    () => Immutable.Map<string, Stream>(streamsList.map((stream) => [stream.id, stream])),
    [streamsList],
  );
  const inputs = useMemo(
    () => Immutable.Map<string, Input>(inputsList.map((input) => [input.id, input])),
    [inputsList],
  );

  const _toggleDetail = useCallback(() => {
    const isSelectingText = !!window.getSelection()?.toString();

    if (!isSelectingText) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_MESSAGE_TABLE_DETAILS_TOGGLED, {
        app_section: 'widget',
        app_action_value: 'widget-message-table-toggle-details',
      });

      toggleDetail(`${message.index}-${message.id}`);
    }
  }, [message.id, message.index, sendTelemetry, toggleDetail]);

  const isSelected = selectedEntities.includes(message.id);
  const checkboxTitle = `${isSelected ? 'Deselect' : 'Select'} message`;
  const isSelectDisabled = !displayBulkSelectCol || !isEntitySelectable(toSelectableMessageTableEntry(message));
  const colSpanFixup = selectedFields.size + 1 + Number(displayBulkSelectCol);

  const selectedFieldsList = useMemo(
    () =>
      selectedFields.toArray().map((selectedFieldName, idx) => {
        const type = fieldType(selectedFieldName, message, fields);

        return (
          <TableDataCell
            className="table-data-cell"
            $isNumeric={type.isNumeric()}
            key={selectedFieldName}
            data-testid={`message-summary-field-${selectedFieldName}`}>
            <Strong strong={idx === 0}>
              <CustomHighlighting field={selectedFieldName} value={message.fields[selectedFieldName]}>
                <TypeSpecificValue
                  value={message.fields[selectedFieldName]}
                  field={selectedFieldName}
                  type={type}
                  render={DecoratedValue}
                />
              </CustomHighlighting>
            </Strong>
          </TableDataCell>
        );
      }),
    [fields, message, selectedFields],
  );

  const messageFieldType = useMemo(() => fieldType(MESSAGE_FIELD, message, fields), [fields, message]);

  return (
    <AdditionalContext.Provider value={additionalContextValue}>
      <TableBody $expanded={expanded} $highlighted={message.id === highlightMessageId}>
        <FieldsRow onClick={_toggleDetail} className="table-data-row">
          {displayBulkSelectCol && (
            <BulkSelectCell onClick={(event) => event.stopPropagation()}>
              <RowCheckbox
                onChange={() => toggleEntitySelect(message.id)}
                title={!isSelectDisabled ? checkboxTitle : undefined}
                checked={isSelected}
                disabled={isSelectDisabled}
                aria-label={checkboxTitle}
              />
            </BulkSelectCell>
          )}
          {selectedFieldsList}
        </FieldsRow>

        <MessagePreview
          showMessageRow={showMessageRow}
          config={config}
          colSpanFixup={colSpanFixup}
          messageFieldType={messageFieldType}
          onRowClick={_toggleDetail}
          message={message}
          displayBulkSelectCol={displayBulkSelectCol}
        />

        {expanded && (
          <MessageDetailRow>
            <td colSpan={colSpanFixup}>
              <MessageDetail
                message={message}
                fields={fields}
                streams={streams}
                allStreams={allStreams}
                inputs={inputs}
                disableSurroundingSearch={disableSurroundingSearch}
                expandAllRenderAsync={expandAllRenderAsync}
              />
            </td>
          </MessageDetailRow>
        )}
      </TableBody>
    </AdditionalContext.Provider>
  );
};

export default MessageTableEntry;
