// @flow strict
import React from 'react';
import connect from 'stores/connect';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import CombinedProvider from 'injection/CombinedProvider';
import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';

import type { StyledComponent } from 'styled-components';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { Message } from './Types';
import { Message as MessagePropType } from './MessagePropTypes';

import MessageDetail from './MessageDetail';

const { InputsStore } = CombinedProvider.get('Inputs');

const ConnectedMessageDetail = connect(
  MessageDetail,
  {
    availableInputs: InputsStore,
    availableStreams: StreamsStore,
    configurations: SearchConfigStore,
  },
  ({ availableStreams = {}, availableInputs = {}, configurations = {}, ...rest }) => {
    const { streams = [] } = availableStreams;
    const { inputs = [] } = availableInputs;
    const { searchesClusterConfig } = configurations;
    return ({
      ...rest,
      allStreams: Immutable.List(streams),
      streams: Immutable.Map(streams.map(stream => [stream.id, stream])),
      inputs: Immutable.Map(inputs.map(input => [input.id, input])),
      searchConfig: searchesClusterConfig,
    });
  },
);

const TableRow: StyledComponent<{}, {}, HTMLTableRowElement> = styled.tr`
  display: 'table-row';

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
  colSpanFixup: number,
  fields: FieldTypeMappingsList,
  message: Message,
  disableSurroundingSearch?: boolean,
  expandAllRenderAsync: boolean,
};

const MessageDetailRow = ({ colSpanFixup, message, fields, disableSurroundingSearch, expandAllRenderAsync }: Props) => {
  return (
    <TableRow>
      <td colSpan={colSpanFixup}>
        <ConnectedMessageDetail message={message}
                                fields={fields}
                                disableSurroundingSearch={disableSurroundingSearch}
                                expandAllRenderAsync={expandAllRenderAsync} />
      </td>
    </TableRow>
  );
};

MessageDetailRow.propTypes = {
  colSpanFixup: PropTypes.number.isRequired,
  disableSurroundingSearch: PropTypes.bool,
  expandAllRenderAsync: PropTypes.bool.isRequired,
  fields: PropTypes.object.isRequired,
  message: MessagePropType.isRequired,
};

MessageDetailRow.defaultProps = {
  disableSurroundingSearch: false,
};

export default MessageDetailRow;
