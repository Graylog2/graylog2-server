import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const ToolsStore = Reflux.createStore({
  testNaturalDate(text) {
    const { url } = ApiRoutes.ToolsApiController.naturalDateTest(text);
    const promise = fetch('GET', URLUtils.qualifyUrl(url));

    promise.catch((errorThrown) => {
      if (errorThrown.additional.status !== 422) {
        UserNotification.error(`Loading keyword preview failed with status: ${errorThrown}`,
          'Could not load keyword preview');
      }
    });

    return promise;
  },
  testGrok(pattern, namedCapturesOnly, string) {
    const { url } = ApiRoutes.ToolsApiController.grokTest();
    const promise = fetch('POST', URLUtils.qualifyUrl(url), {
      pattern: pattern,
      string: string,
      named_captures_only: namedCapturesOnly,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'We were not able to run the grok extraction. Please check your parameters.');
    });

    return promise;
  },
  testJSON(flatten, listSeparator, keySeparator, kvSeparator, replaceKeyWhitespace, keyWhitespaceReplacement, keyPrefix, string) {
    const { url } = ApiRoutes.ToolsApiController.jsonTest();
    const payload = {
      flatten: flatten,
      list_separator: listSeparator,
      key_separator: keySeparator,
      kv_separator: kvSeparator,
      replace_key_whitespace: replaceKeyWhitespace,
      key_whitespace_replacement: keyWhitespaceReplacement,
      key_prefix: keyPrefix,
      string: string,
    };

    const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'We were not able to run the JSON extraction. Please check your parameters.');
    });

    return promise;
  },
  testRegex(regex, string) {
    const { url } = ApiRoutes.ToolsApiController.regexTest();
    const promise = fetch('POST', URLUtils.qualifyUrl(url), {
      regex: regex,
      string: string,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'Could not try regular expression. Make sure that it is valid.');
    });

    return promise;
  },
  testRegexReplace(regex, replacement, replaceAll, string) {
    const { url } = ApiRoutes.ToolsApiController.regexReplaceTest();
    const payload = {
      regex: regex,
      replacement: replacement,
      replace_all: replaceAll,
      string: string,
    };
    const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'Could not try regular expression. Make sure that it is valid.');
    });

    return promise;
  },
  testSplitAndIndex(splitBy, index, string) {
    const { url } = ApiRoutes.ToolsApiController.splitAndIndexTest();
    const payload = {
      split_by: splitBy,
      index: index,
      string: string,
    };

    const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'We were not able to run the split and index extraction. Please check your parameters.');
    });

    return promise;
  },
  testSubstring(beginIndex, endIndex, string) {
    const { url } = ApiRoutes.ToolsApiController.substringTest();
    const payload = {
      start: beginIndex,
      end: endIndex,
      string: string,
    };

    const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'We were not able to run the substring extraction. Please check index boundaries.');
    });

    return promise;
  },
  testContainsString(searchString, string) {
    const { url } = ApiRoutes.ToolsApiController.containsStringTest();
    const promise = fetch('POST', URLUtils.qualifyUrl(url), {
      search_string: searchString,
      string: string,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`,
        'Could not check if field contains the string');
    });

    return promise;
  },

  testLookupTable(lookupTableName, string) {
    const { url } = ApiRoutes.ToolsApiController.lookupTableTest();
    const promise = fetch('POST', URLUtils.qualifyUrl(url), {
      lookup_table_name: lookupTableName,
      string: string,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`, 'Could not check if lookup table translates the string');
    });

    return promise;
  },
});

export default ToolsStore;
