import React, {PropTypes} from 'react';
import InputDropdown from 'components/inputs/InputDropdown';
import UserNotification from 'util/UserNotification';

import UniversalSearchStore from 'stores/search/UniversalSearchStore';

const RecentMessageLoader = React.createClass({
  propTypes: {
    inputs: PropTypes.object,
    onMessageLoaded: PropTypes.func.isRequired,
    selectedInputId: PropTypes.string,
  },
  onClick(inputId) {
    const input = this.props.inputs.get(inputId);
    if (!input) {
      UserNotification.error('Invalid input selected: ' + inputId,
        'Could not load message from invalid Input ' + inputId);
    }
    UniversalSearchStore.search('relative', 'gl2_source_input:' + inputId + ' OR gl2_source_radio_input:' + inputId, { range: 0 }, 1)
      .then((response) => {
        if (response.total_results > 0) {
          this.props.onMessageLoaded(response.messages[0]);
        } else {
          UserNotification.error('Input did not return a recent message.');
          this.props.onMessageLoaded(undefined);
        }
      });
  },
  render() {
    let helpMessage;
    if (this.props.selectedInputId) {
      helpMessage = 'Click on "Load Message" to load the most recent message from this input.';
    } else {
      helpMessage = 'Select an Input from the list below and click "Load Message" to load the most recent message from this input.';
    }
    return (
      <div style={{marginTop: 5}}>
        {helpMessage}
        <InputDropdown inputs={this.props.inputs} preselectedInputId={this.props.selectedInputId} onLoadMessage={this.onClick} title="Load Message"/>
      </div>
    );
  },
});

export default RecentMessageLoader;
