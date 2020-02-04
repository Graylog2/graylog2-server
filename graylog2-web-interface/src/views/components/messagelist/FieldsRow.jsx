// @flow strict
import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import FieldType from 'views/logic/fieldtypes/FieldType';
import type { StyledComponent } from 'styled-components';
import type { Message } from './Types';
import { Message as MessagePropType } from './MessagePropTypes';

import CustomHighlighting from './CustomHighlighting';
import TypeSpecificValue from '../TypeSpecificValue';
import DecoratedValue from './decoration/DecoratedValue';

const TableRow: StyledComponent<{}, {}, HTMLTableRowElement> = styled.tr`
  cursor: pointer;

  td {
    border: 0;
    padding-top: 10px;
  }
  .timezoneInfo {
    color: #aaa;
  }
`;

const _renderStrong = (children, strong = false) => {
  if (strong) {
    return <strong>{children}</strong>;
  }
  return children;
};

type Props = {
  message: Message,
  selectedFields?: Immutable.OrderedSet<string>,
  getFieldType: (fieldName: string) => FieldType,
  onClick: () => void,
};

const FieldsRow = ({ selectedFields, message, getFieldType, onClick }: Props) => {
  return (
    <TableRow onClick={() => onClick()}>
      {selectedFields.toArray().map((selectedFieldName, idx) => {
        const type = getFieldType(selectedFieldName);
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
    </TableRow>
  );
};


FieldsRow.propTypes = {
  message: MessagePropType.isRequired,
  onClick: PropTypes.func.isRequired,
  selectedFields: PropTypes.instanceOf(Immutable.OrderedSet),
};

FieldsRow.defaultProps = {
  selectedFields: Immutable.OrderedSet(),
};

export default FieldsRow;
