import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import MessageFormatter from 'logic/message/MessageFormatter';
import ObjectUtils from 'util/ObjectUtils';
import CombinedProvider from 'injection/CombinedProvider';

const { SimulatorActions } = CombinedProvider.get('Simulator');

const SimulatorStore = Reflux.createStore({
  listenables: [SimulatorActions],

  simulate(stream, messageFields, inputId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.SimulatorController.simulate().url);
    const simulation = {
      stream_id: stream.id,
      message: messageFields,
      input_id: inputId,
    };

    let promise = fetch('POST', url, simulation);
    promise = promise.then((response) => {
      const formattedResponse = ObjectUtils.clone(response);
      formattedResponse.messages = response.messages.map((msg) => MessageFormatter.formatMessageSummary(msg));

      return formattedResponse;
    });

    SimulatorActions.simulate.promise(promise);
  },
});

export default SimulatorStore;
