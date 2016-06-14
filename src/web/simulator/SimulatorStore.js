import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import MessageFormatter from 'logic/message/MessageFormatter';
import ObjectUtils from 'util/ObjectUtils';

import SimulatorActions from './SimulatorActions';

const urlPrefix = '/plugins/org.graylog.plugins.pipelineprocessor';

const SimulatorStore = Reflux.createStore({
  listenables: [SimulatorActions],

  simulate(stream, index, messageId) {
    const url = URLUtils.qualifyUrl(`${urlPrefix}/system/pipelines/simulate`);
    const simulation = {
      stream_id: stream.id,
      index: index,
      message_id: messageId,
    };

    let promise = fetch('POST', url, simulation);
    promise = promise.then(response => {
      const formattedResponse = ObjectUtils.clone(response);
      formattedResponse.messages = response.messages.map(MessageFormatter.formatMessageSummary);

      return formattedResponse;
    });

    SimulatorActions.simulate.promise(promise);
  },
});

export default SimulatorStore;
