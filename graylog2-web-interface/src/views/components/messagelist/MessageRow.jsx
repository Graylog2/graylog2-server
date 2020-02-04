// @flow strict
import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

import FieldType from 'views/logic/fieldtypes/FieldType';
import type { StyledComponent } from 'styled-components';
import { Message as MessagePropType } from './MessagePropTypes';
import type { Message } from './Types';

import TypeSpecificValue from '../TypeSpecificValue';
import DecoratedValue from './decoration/DecoratedValue';
import CustomHighlighting from './CustomHighlighting';

type Props = {
  colSpanFixup: number,
  fieldType: FieldType,
  message: Message,
  onClick: () => void,
};

const MessageWrapper = styled.div`
  line-height: 1.5em;
  white-space: pre-line;
  max-height: 6em; /* show 4 lines: line-height * 4 */
  overflow: hidden;
`;

const TableRow: StyledComponent<{}, {}, HTMLTableRowElement> = styled.tr`
  && {
    margin-bottom: 5px;
    cursor: pointer;
  
    td {
      border-top: 0;
      padding-top: 0;
      padding-bottom: 5px;
      font-family: monospace;
      color: #16ace3;
    }
  }
`;

const MessageRow = ({ colSpanFixup, message, fieldType, onClick }: Props) => {
  return (
    <TableRow onClick={() => onClick()}>
      <td colSpan={colSpanFixup}>
        <MessageWrapper>
          <CustomHighlighting field="message" value={message.fields.message}>
            <TypeSpecificValue field="message" value={message.fields.message} type={fieldType} render={DecoratedValue} />
          </CustomHighlighting>
        </MessageWrapper>
      </td>
    </TableRow>
  );
};

MessageRow.propTypes = {
  colSpanFixup: PropTypes.number.isRequired,
  message: MessagePropType.isRequired,
  onClick: PropTypes.func.isRequired,
};

export default MessageRow;
