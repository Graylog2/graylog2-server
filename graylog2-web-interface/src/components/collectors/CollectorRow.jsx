import React from 'react';
import moment from 'moment';
import momentHelper from 'legacy/moment-helper';
import jsRoutes from 'routing/jsRoutes';

const CollectorRow = React.createClass({
  propTypes: {
    collector: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      showRelativeTime: true,
    };
  },
  _getOsGlyph(operatingSystem) {
    let glyphClass = 'fa-question-circle';
    const os = operatingSystem.trim().toLowerCase();
    if (os.indexOf('mac os') > -1) {
      glyphClass = 'fa-apple';
    }
    if (os.indexOf('linux') > -1) {
      glyphClass = 'fa-linux';
    }
    if (os.indexOf('win') > -1) {
      glyphClass = 'fa-windows';
    }

    glyphClass += ' collector-os';

    return (<i className={'fa ' + glyphClass}/>);
  },
  render() {
    const collector = this.props.collector;
    const collectorClass = collector.active ? '' : 'greyed-out inactive';
    const style = {};
    const annotation = collector.active ? '' : '(inactive)';
    const osGlyph = this._getOsGlyph(collector.node_details.operating_system);
    const formattedTime = (this.state.showRelativeTime ? moment(collector.last_seen).fromNow() : momentHelper.toUserTimeZone(moment(collector.last_seen)).format());
    return (
      <tr className={collectorClass} style={style}>
        <td className="limited">
          {collector.node_id}
        </td>
        <td className="limited">
          {osGlyph}
          {collector.node_details.operating_system}
        </td>
        <td className="limited">
          <time dateTime={collector.last_seen} title={momentHelper.toUserTimeZone(moment(collector.last_seen)).format()}>{formattedTime}</time>
        </td>
        <td className="limited">
          {collector.id}
          {annotation}
        </td>
        <td className="limited">
          {collector.collector_version}
        </td>
        <td className="limited">
          <a href={jsRoutes.controllers.SearchController.index('gl2_source_collector:' + collector.id, 'relative', 28800).url}
             className="btn btn-info btn-xs">Show messages from this collector</a>
        </td>
      </tr>
    );
  },
});

export default CollectorRow;
