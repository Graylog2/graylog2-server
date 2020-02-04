// @flow strict
import React from 'react';
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Message } from './Types';
import { Message as MessagePropType } from './MessagePropTypes';

import MessageRow from './MessageRow';
import FieldsRow from './FieldsRow';
import MessageDetailRow from './MessageDetailRow';

const TableBody: StyledComponent<{ expanded: boolean, highlighted: boolean }, {}, HTMLTableSectionElement> = styled.tbody(
  ({ expanded, highlighted }) => css`
    && {
      border-top: 0;
    }

    ${expanded && css`
      border-left: 7px solid #16ace3;
    `}
    
    ${highlighted && css`
      border-left: 7px solid #8dc63f;
    `}  
  `,
);

const isDecoratedField = (field, decorationStats) => decorationStats && (decorationStats.added_fields[field] !== undefined || decorationStats.changed_fields[field] !== undefined);

const getFieldType = (fieldName, { decoration_stats: decorationStats }, fields) => (isDecoratedField(fieldName, decorationStats)
  ? FieldType.Decorated
  : fields.find(t => t.name === fieldName, undefined, FieldTypeMapping.create(fieldName, FieldType.Unknown)).type);

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

const MessageTableEntry = ({
  disableSurroundingSearch,
  expandAllRenderAsync,
  expanded,
  fields,
  highlightMessage = '',
  message,
  showMessageRow = false,
  selectedFields = Immutable.OrderedSet(),
  toggleDetail,
}: Props) => {
  const _toggleDetail = () => {
    toggleDetail(`${message.index}-${message.id}`);
  };
  const colSpanFixup = selectedFields.size + 1;
  const highlighted = message.id === highlightMessage;

  return (
    <TableBody expanded={expanded} highlighted={highlighted}>
      <FieldsRow getFieldType={selectedFieldName => getFieldType(selectedFieldName, message, fields)}
                 message={message}
                 onClick={_toggleDetail}
                 selectedFields={selectedFields} />
      {showMessageRow && (
        <MessageRow onClick={_toggleDetail}
                    colSpanFixup={colSpanFixup}
                    fieldType={getFieldType('message', message, fields)}
                    message={message} />
      )}
      {expanded && (
        <MessageDetailRow disableSurroundingSearch={disableSurroundingSearch}
                          expandAllRenderAsync={expandAllRenderAsync}
                          fields={fields}
                          message={message}
                          colSpanFixup={colSpanFixup} />
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
  message: MessagePropType.isRequired,
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
