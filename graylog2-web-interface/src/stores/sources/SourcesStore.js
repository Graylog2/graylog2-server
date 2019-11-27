// @flow strict
import Reflux from 'reflux';
import $ from 'jquery';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import StringUtils from 'util/StringUtils';

type Source = {
  name: string,
  message_count: number,
  percentage: number,
};

const processSourcesData = (sources: { string: number }): Array<Source> => {
  let total = 0;
  const sourcesArray = [];
  $.each(sources, (name: string, count: number) => {
    total += Number(count);
    sourcesArray.push({
      name: StringUtils.escapeHTML(name),
      message_count: count,
      percentage: 0,
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

  loadSources(range: number, callback: (sources: Array<Source>) => void) {
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
