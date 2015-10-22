import React, {PropTypes} from 'react';
import InputDropdown from 'components/inputs/InputDropdown';
import InputsStore from 'stores/inputs/InputsStore';
import UserNotification from 'util/UserNotification';

const RecentMessageLoader = React.createClass({
  propTypes: {
    inputs: PropTypes.object,
    onMessageLoaded: PropTypes.func.isRequired,
  },
  onClick(inputId) {
    const input = this.props.inputs.get(inputId);
    if (!input) {
      UserNotification.error('Invalid input selected: ' + inputId,
        'Could not load message from invalid Input ' + inputId);
    }
    InputsStore.globalRecentMessage(input, (message) => {
      message.source_input_id = input.id;
      this.props.onMessageLoaded(message);
    });
  },
  render() {
    return (
      <div style={{marginTop: 5}}>
        Select an Input from the list below and click "Load Message" to load the most recent message from this input.
        <InputDropdown inputs={this.props.inputs} onLoadMessage={this.onClick} title="Load Message"/>
      </div>
    );
  }
});

export default RecentMessageLoader;
