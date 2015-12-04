import React from 'react';
import { AlarmCallback } from 'components/alarmcallbacks';

const AlarmCallbackHistory = React.createClass({
  propTypes: {
    types: React.PropTypes.object.isRequired,
    alarmCallbackHistory: React.PropTypes.object.isRequired,
  },
  _greenTick() {return <i className="fa fa-check" style={{color: 'green'}}/>;},
  _redCross() {return <i className="fa fa-close" style={{color: 'red'}}/>;},
  render() {
    const history = this.props.alarmCallbackHistory;
    const result = (history.result.type === 'error' ? this._redCross() : this._greenTick());
    const subtitle = (history.result.type === 'error' ? <div style={{color: 'red'}}>{history.result.error}</div> : null);
    return (
      <AlarmCallback alarmCallback={history.alarmcallbackconfiguration} types={this.props.types}
                     titleAnnotation={result} subtitle={subtitle} concise/>
    );
  },
});

export default AlarmCallbackHistory;
