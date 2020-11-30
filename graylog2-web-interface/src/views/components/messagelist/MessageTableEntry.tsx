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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import { Store } from 'stores/StoreTypes';

import MessageDetail from './MessageDetail';
import DecoratedValue from './decoration/DecoratedValue';
import CustomHighlighting from './CustomHighlighting';
import style from './MessageTableEntry.css';
import type { Message } from './Types';

import TypeSpecificValue from '../TypeSpecificValue';

const { InputsStore } = CombinedProvider.get('Inputs');

type Stream = { id: string };
type Input = { id: string };

type InputsStoreState = {
  inputs: Array<Input>;
};

const ConnectedMessageDetail = connect(
  MessageDetail,
  {
    availableInputs: InputsStore as Store<InputsStoreState>,
    availableStreams: StreamsStore,
    configurations: SearchConfigStore,
  },
  ({ availableStreams = {}, availableInputs = {}, configurations = {}, ...rest }) => {
    const { streams = [] } = availableStreams;
    const { inputs = [] } = availableInputs;
    const { searchesClusterConfig } = configurations;

    return ({
      ...rest,
      allStreams: Immutable.List<Stream>(streams),
      streams: Immutable.Map<string, Stream>(streams.map((stream) => [stream.id, stream])),
      inputs: Immutable.Map<string, Input>(inputs.map((input) => [input.id, input])),
      searchConfig: searchesClusterConfig,
    });
  },
);

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

  let classes = 'message-group';

  if (expanded) {
    classes += ' message-group-toggled';
  }

  if (message.id === highlightMessage) {
    classes += ' message-highlight';
  }

  return (
    <tbody className={classes}>
      <tr className="fields-row" onClick={_toggleDetail}>
        { selectedFields.toArray().map((selectedFieldName, idx) => {
          const type = fieldType(selectedFieldName, message, fields);

          return (
            <td className={style.fieldsRowField} key={selectedFieldName}>
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
        }) }
      </tr>

      {showMessageRow
      && (
        <tr className="message-row" onClick={_toggleDetail}>
          <td colSpan={colSpanFixup}>
            <div className="message-wrapper">
              <CustomHighlighting field="message" value={message.fields.message}>
                <TypeSpecificValue field="message" value={message.fields.message} type={fieldType('message', message, fields)} render={DecoratedValue} />
              </CustomHighlighting>
            </div>
          </td>
        </tr>
      )}
      {expanded
      && (
        <tr className="message-detail-row" style={{ display: 'table-row' }}>
          <td colSpan={colSpanFixup}>
            {/* @ts-ignore */}
            <ConnectedMessageDetail message={message}
                                    fields={fields}
                                    disableSurroundingSearch={disableSurroundingSearch}
                                    expandAllRenderAsync={expandAllRenderAsync} />
          </td>
        </tr>
      )}
    </tbody>
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
