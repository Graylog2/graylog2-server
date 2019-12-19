// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import connect from 'stores/connect';

import CombinedProvider from 'injection/CombinedProvider';

import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';

import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import MessageDetail from './MessageDetail';
import TypeSpecificValue from '../TypeSpecificValue';
import DecoratedValue from './decoration/DecoratedValue';
import CustomHighlighting from './CustomHighlighting';

import style from './MessageTableEntry.css';
import type { Message } from './Types';

const { NodesStore } = CombinedProvider.get('Nodes');
const { InputsStore } = CombinedProvider.get('Inputs');

const ConnectedMessageDetail = connect(
  MessageDetail,
  {
    availableInputs: InputsStore,
    availableNodes: NodesStore,
    availableStreams: StreamsStore,
    configurations: SearchConfigStore,
  },
  ({ availableStreams = {}, availableNodes = {}, availableInputs = {}, configurations = {}, ...rest }) => {
    const { streams = [] } = availableStreams;
    const { nodes } = availableNodes;
    const { inputs = [] } = availableInputs;
    const { searchesClusterConfig } = configurations;
    return ({
      ...rest,
      allStreams: Immutable.List(streams),
      streams: Immutable.Map(streams.map(stream => [stream.id, stream])),
      nodes: Immutable.Map(nodes),
      inputs: Immutable.Map(inputs.map(input => [input.id, input])),
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

const fieldType = (fieldName, { decoration_stats: decorationStats}, fields) => isDecoratedField(fieldName, decorationStats)
  ? FieldType.Decorated
  : fields.find(t => t.name === fieldName, undefined, FieldTypeMapping.create(fieldName, FieldType.Unknown)).type;

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
                <DecoratedValue field="message" value={message.fields.message} type={fieldType('message', message, fields)} />
              </CustomHighlighting>
            </div>
          </td>
        </tr>
      )}
      {expanded
      && (
        <tr className="message-detail-row" style={{ display: 'table-row' }}>
          <td colSpan={colSpanFixup}>
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
