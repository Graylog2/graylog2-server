import PropTypes from 'prop-types';
import React from 'react';
import Immutable from 'immutable';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { Popover, OverlayTrigger } from 'react-bootstrap';

import MessageDetail from './MessageDetail';
import { Timestamp } from 'components/common';
import StringUtils from 'util/StringUtils';
import DateTime from 'logic/datetimes/DateTime';
import style from './MessageTableEntry.css';

class MessageTableEntry extends React.Component {
  static propTypes = {
    allStreams: ImmutablePropTypes.list.isRequired,
    allStreamsLoaded: PropTypes.bool.isRequired,
    disableSurroundingSearch: PropTypes.bool,
    expandAllRenderAsync: PropTypes.bool.isRequired,
    expanded: PropTypes.bool.isRequired,
    highlight: PropTypes.bool,
    highlightMessage: PropTypes.string,
    inputs: ImmutablePropTypes.map.isRequired,
    message: PropTypes.shape({
      fields: PropTypes.object.isRequired,
      highlight_ranges: PropTypes.object,
      id: PropTypes.string.isRequired,
      index: PropTypes.string.isRequired,
    }).isRequired,
    nodes: ImmutablePropTypes.map.isRequired,
    searchConfig: PropTypes.object,
    selectedFields: ImmutablePropTypes.orderedSet,
    showMessageRow: PropTypes.bool,
    streams: ImmutablePropTypes.map.isRequired,
    toggleDetail: PropTypes.func.isRequired,
  };

  static defaultProps = {
    disableSurroundingSearch: false,
    highlight: false,
    highlightMessage: undefined,
    searchConfig: undefined,
    selectedFields: Immutable.OrderedSet(),
    showMessageRow: false,
  };

  shouldComponentUpdate(newProps) {
    if (this.props.highlight !== newProps.highlight) {
      return true;
    }
    if (!Immutable.is(this.props.selectedFields, newProps.selectedFields)) {
      return true;
    }
    if (this.props.expanded !== newProps.expanded) {
      return true;
    }
    if (this.props.expandAllRenderAsync !== newProps.expandAllRenderAsync) {
      return true;
    }
    if (this.props.allStreamsLoaded !== newProps.allStreamsLoaded) {
      return true;
    }
    if (this.props.showMessageRow !== newProps.showMessageRow) {
      return true;
    }
    return false;
  }

  renderForDisplay = (fieldName, truncate) => {
    const fullOrigValue = this.props.message.fields[fieldName];

    if (fullOrigValue === undefined) {
      return '';
    }

    /* Timestamp can not be highlighted by elastic search. So we can safely
     * skip them from highlighting. */
    if (fieldName === 'timestamp') {
      return this._toTimestamp(fullOrigValue);
    } else {
      return this.possiblyHighlight(fieldName, fullOrigValue, truncate);
    }
  };

  possiblyHighlight = (fieldName, fullOrigValue, truncate) => {
    // Ensure the field is a string for later processing
    const fullStringOrigValue = StringUtils.stringify(fullOrigValue);

    // Truncate the field to 2048 characters if requested. This is for performance reasons to avoid hogging the CPU.
    // It's not optimal, more like a workaround to at least being able to show the page...
    const origValue = truncate ? fullStringOrigValue.slice(0, 2048) : fullStringOrigValue;

    if (this.props.highlight && this.props.message.highlight_ranges) {
      if (this.props.message.highlight_ranges.hasOwnProperty(fieldName)) {
        const chunks = [];
        const highlights = Immutable.fromJS(this.props.message.highlight_ranges[fieldName]).sortBy(range => range.get('start'));
        let position = 0;
        let key = 0;
        highlights.forEach((range, idx) => {
          if (position !== range.get('start')) {
            chunks.push(<span key={key++}>{origValue.substring(position, range.get('start'))}</span>);
          }
          chunks.push(<span key={key++} className="result-highlight-colored">{origValue.substring(range.get('start'), range.get('start') + range.get('length'))}</span>);
          if ((idx + 1) < highlights.size) {
            const nextRange = highlights.get(idx + 1);
            chunks.push(<span key={key++}>{origValue.substring(range.get('start') + range.get('length'), nextRange.get('start'))}</span>);
            position = nextRange.get('start');
          } else {
            chunks.push(<span key={key++}>{origValue.substring(range.get('start') + range.get('length'))}</span>);
            position = range.get('start') + range.get('length');
          }
        });
        return <span>{chunks}</span>;
      }
      return String(origValue);
    }
    return String(origValue);
  };

  _toggleDetail = () => {
    this.props.toggleDetail(`${this.props.message.index}-${this.props.message.id}`);
  };

  _toTimestamp = (value) => {
    const popoverHoverFocus = (
      <Popover id="popover-trigger-hover-focus">
        This timestamp is rendered in your timezone.
      </Popover>
    );

    return (
      <span>
        <Timestamp dateTime={value} format={DateTime.Formats.TIMESTAMP_TZ} />
        <OverlayTrigger trigger={['hover']} overlay={popoverHoverFocus} >
          <i className={`fa fa-fw fa-info ${style.timezoneInfo}`} />
        </OverlayTrigger>
      </span>
    );
  };

  render() {
    const colSpanFixup = this.props.selectedFields.size + 1;

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
          <td><strong>
            <Timestamp dateTime={this.props.message.fields.timestamp} />
          </strong></td>
          { this.props.selectedFields.toSeq().map(selectedFieldName => (<td
            key={selectedFieldName}>{this.renderForDisplay(selectedFieldName, true)} </td>)) }
        </tr>

        {this.props.showMessageRow &&
        <tr className="message-row" onClick={this._toggleDetail}>
          <td colSpan={colSpanFixup}><div className="message-wrapper">{this.renderForDisplay('message', true)}</div></td>
        </tr>
        }
        {this.props.expanded &&
        <tr className="message-detail-row" style={{ display: 'table-row' }}>
          <td colSpan={colSpanFixup}>
            <MessageDetail message={this.props.message}
                           inputs={this.props.inputs}
                           streams={this.props.streams}
                           allStreams={this.props.allStreams}
                           allStreamsLoaded={this.props.allStreamsLoaded}
                           nodes={this.props.nodes}
                           renderForDisplay={this.renderForDisplay}
                           disableSurroundingSearch={this.props.disableSurroundingSearch}
                           expandAllRenderAsync={this.props.expandAllRenderAsync}
                           searchConfig={this.props.searchConfig} />
          </td>
        </tr>
        }
      </tbody>
    );
  }
}

export default MessageTableEntry;
