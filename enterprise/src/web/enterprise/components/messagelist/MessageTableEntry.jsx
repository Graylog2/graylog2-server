import PropTypes from 'prop-types';
import * as React from 'react';
import * as Immutable from 'immutable';
import connect from 'stores/connect';

import CombinedProvider from 'injection/CombinedProvider';

import { StreamsStore } from 'enterprise/stores/StreamsStore';
import { SearchConfigStore } from 'enterprise/stores/SearchConfigStore';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import MessageDetail from './MessageDetail';
import TypeSpecificValue from '../TypeSpecificValue';
import Highlight from './Highlight';
import style from './MessageTableEntry.css';

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

class MessageTableEntry extends React.Component {
  static propTypes = {
    disableSurroundingSearch: PropTypes.bool,
    expandAllRenderAsync: PropTypes.bool.isRequired,
    expanded: PropTypes.bool.isRequired,
    fields: PropTypes.object.isRequired,
    highlight: PropTypes.bool,
    highlightMessage: PropTypes.string,
    message: PropTypes.shape({
      fields: PropTypes.object.isRequired,
      highlight_ranges: PropTypes.object,
      id: PropTypes.string.isRequired,
      index: PropTypes.string.isRequired,
    }).isRequired,
    selectedFields: PropTypes.instanceOf(Immutable.OrderedSet),
    showMessageRow: PropTypes.bool,
    toggleDetail: PropTypes.func.isRequired,
  };

  static defaultProps = {
    disableSurroundingSearch: false,
    highlight: false,
    highlightMessage: undefined,
    selectedFields: Immutable.OrderedSet(),
    showMessageRow: false,
  };

  _toggleDetail = () => {
    this.props.toggleDetail(`${this.props.message.index}-${this.props.message.id}`);
  };

  _renderStrong = (children, strong = false) => {
    if (strong) {
      return <strong>{children}</strong>;
    }
    return children;
  };

  render() {
    const colSpanFixup = this.props.selectedFields.size + 1;
    const { message } = this.props;

    let classes = 'message-group';
    if (this.props.expanded) {
      classes += ' message-group-toggled';
    }
    if (this.props.message.id === this.props.highlightMessage) {
      classes += ' message-highlight';
    }
    return (
      <tbody className={classes}>
        <tr className="fields-row" onClick={this._toggleDetail}>
          { this.props.selectedFields.toArray().map((selectedFieldName, idx) => {
            const fieldTypeMapping = this.props.fields.find(type => type.name === selectedFieldName);
            const fieldType = fieldTypeMapping ? fieldTypeMapping.type : FieldType.Unknown;
            return (<td className={style.fieldsRowField} key={selectedFieldName}>
              {this._renderStrong(
                <TypeSpecificValue value={message.fields[selectedFieldName]}
                                   type={fieldType}
                                   render={({ value }) => <Highlight field={selectedFieldName} value={value} />} />,
                idx === 0)}
            </td>);
          }) }
        </tr>

        {this.props.showMessageRow
        && (
        <tr className="message-row" onClick={this._toggleDetail}>
          <td colSpan={colSpanFixup}><div className="message-wrapper"><Highlight field="message" value={message.fields.message} /></div></td>
        </tr>
        )}
        {this.props.expanded
        && (
        <tr className="message-detail-row" style={{ display: 'table-row' }}>
          <td colSpan={colSpanFixup}>
            <ConnectedMessageDetail message={message}
                                    fields={this.props.fields}
                                    disableSurroundingSearch={this.props.disableSurroundingSearch}
                                    expandAllRenderAsync={this.props.expandAllRenderAsync} />
          </td>
        </tr>
        )}
      </tbody>
    );
  }
}

export default MessageTableEntry;
