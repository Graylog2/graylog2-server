import Reflux from 'reflux';
import $ from 'jquery';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import StringUtils from 'util/StringUtils';

const processSourcesData = (sources) => {
  let total = 0;
  const sourcesArray = [];
  $.each(sources, (name, count) => {
    total += Number(count);
    sourcesArray.push({
      name: StringUtils.escapeHTML(name),
      message_count: count,
    });
  });
  sourcesArray.forEach((d) => {
    // eslint-disable-next-line no-param-reassign
    d.percentage = d.message_count / total * 100;
  });
  return sourcesArray;
};

const SourcesStore = Reflux.createStore({
  SOURCES_URL: '/sources',

  loadSources(range, callback) {
    let url = URLUtils.qualifyUrl(this.SOURCES_URL);
    if (typeof range !== 'undefined') {
      url += `?range=${range}`;
    }
    fetch('GET', url)
      .then((response) => {
        const sources = processSourcesData(response.sources);
        callback(sources);
      })
      .catch((errorThrown) => {
        UserNotification.error(`Loading of sources data failed with status: ${errorThrown}. Try reloading the page.`,
          'Could not load sources data');
      });
  },
});

export default SourcesStore;
