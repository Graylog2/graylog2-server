// @flow strict
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { RefreshActions } from 'views/stores/RefreshStore';

import { AdditionalContext } from 'views/logic/ActionContext';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import FieldType from 'views/logic/fieldtypes/FieldType';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';

import CustomPropTypes from 'views/components/CustomPropTypes';
import { MessageTableEntry } from 'views/components/messagelist';
import Field from 'views/components/Field';

const Table = styled.table`
  position: relative;
  font-size: 11px;
  margin-top: 0;
  margin-bottom: 60px;
  border-collapse: collapse;
  padding-left: 13px;
  width: 100%;
  word-break: break-all;

  @media print {
    font-size: 14px;
    padding-left: 0;
    min-width: 50%;
    
    td {
      border: 1px #ccc solid !important;
      left: 0;
      padding: 5px;
      position: static;
    }
  }
`;

const TableHeadCell = styled.th`
  background-color: #eee;
  border: 0;
  color: #333;
  font-size: 11px;
  font-weight: normal;
  white-space: nowrap;

  @media print {    
    font-weight: bold !important;
    font-size: inherit !important;
    border: 1px #ccc solid !important;
    left: 0;
    padding: 5px;
    position: static;
  }
`;

const _columnStyle = (fieldName: string, fields: Immutable.List) => {
  const selectedFields = Immutable.OrderedSet(fields);
  if (fieldName.toLowerCase() === 'source' && selectedFields.size > 1) {
    return { width: 180 };
  }
  return {};
};

const _fieldTypeFor = (fieldName: string, fields: Immutable.List) => {
  return (fields.find(f => f.name === fieldName) || { type: FieldType.Unknown }).type;
};

const _getFormattedMessages = (messages: Array<Object>): Array<Object> => {
  return messages.map(m => ({
    fields: m.message,
    formatted_fields: MessageFieldsFilter.filterFields(m.message),
    id: m.message._id,
    index: m.index,
    highlight_ranges: m.highlight_ranges,
    decoration_stats: m.decoration_stats,
  }));
};

const _getSelectedFields = (selectedFields?: Object, config: MessagesWidgetConfig) => {
  if (config) {
    return Immutable.Set(config.fields);
  }
  return selectedFields;
};

const _toggleMessageDetail = (id: string, expandedMessages: Immutable.Set, setExpandedMessages: (newSet: Immutable.Set) => void) => {
  let newSet;
  if (expandedMessages.contains(id)) {
    newSet = expandedMessages.delete(id);
  } else {
    newSet = expandedMessages.add(id);
    RefreshActions.disable();
  }
  setExpandedMessages(newSet);
};

type Props = {
  fields: Immutable.List,
  config: MessagesWidgetConfig,
  selectedFields?: Object,
  activeQueryId: string,
  messages: Array<Object>
};

const MessageTable = ({ fields, activeQueryId, config, messages, selectedFields: selectedFieldsProp }: Props) => {
  const [expandedMessages, setExpandedMessages] = useState(Immutable.Set());
  const formattedMessages = _getFormattedMessages(messages);
  const selectedFields = _getSelectedFields(selectedFieldsProp, config);
  const selectedColumns = Immutable.OrderedSet(selectedFields);

  return (
    <div className="table-responsive">
      <Table className="table table-condensed">
        <thead>
          <tr>
            {selectedColumns.toSeq().map((selectedFieldName) => {
              return (
                <TableHeadCell key={selectedFieldName}
                               style={_columnStyle(selectedFieldName, fields)}>
                  <Field type={_fieldTypeFor(selectedFieldName, fields)}
                         name={selectedFieldName}
                         queryId={activeQueryId} />
                </TableHeadCell>
              );
            })}
          </tr>
        </thead>
        {formattedMessages.map((message) => {
          const messageKey = `${message.index}-${message.id}`;
          return (
            <AdditionalContext.Provider key={messageKey}
                                        value={{ message }}>
              <MessageTableEntry fields={fields}
                                 disableSurroundingSearch
                                 message={message}
                                 showMessageRow={config && config.showMessageRow}
                                 selectedFields={selectedColumns}
                                 expanded={expandedMessages.contains(messageKey)}
                                 toggleDetail={id => _toggleMessageDetail(id, expandedMessages, setExpandedMessages)}
                                 highlight
                                 expandAllRenderAsync={false} />
            </AdditionalContext.Provider>
          );
        })}
      </Table>
    </div>
  );
};

MessageTable.propTypes = {
  activeQueryId: PropTypes.string.isRequired,
  config: CustomPropTypes.instanceOf(MessagesWidgetConfig).isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  messages: PropTypes.arrayOf(PropTypes.object).isRequired,
  selectedFields: PropTypes.object,
};

MessageTable.defaultProps = {
  selectedFields: Immutable.Set(),
};

export default MessageTable;
