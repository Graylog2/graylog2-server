/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import URI from 'urijs';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

type NaturalDateResponse = {
  from: string;
  to: string;
  timezone: string;
};

const ToolsStore = {
  testNaturalDate(keyword: string, userTimezone: string): Promise<NaturalDateResponse> {
    const { url } = ApiRoutes.ToolsApiController.naturalDateTest(
      encodeURIComponent(keyword),
      encodeURIComponent(userTimezone),
    );
    const promise = fetch('GET', qualifyUrl(url));

    promise.catch((errorThrown) => {
      if (errorThrown.additional.status !== 422) {
        UserNotification.error(
          `Loading keyword preview failed with status: ${errorThrown}`,
          'Could not load keyword preview',
        );
      }
    });

    return promise;
  },
  testGrok(
    pattern: string,
    namedCapturesOnly: boolean,
    string: string,
  ): Promise<{
    pattern: string;
    string: string;
    named_captures_only: string;
    error_message: string;
    matched: boolean;
    matches: Array<{ name: string; match: string }>;
  }> {
    const { url } = ApiRoutes.ToolsApiController.grokTest();
    const promise = fetch('POST', qualifyUrl(url), {
      pattern: pattern,
      string: string,
      named_captures_only: namedCapturesOnly,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'We were not able to run the grok extraction. Please check your parameters.',
      );
    });

    return promise;
  },
  testJSON(
    flatten: boolean,
    listSeparator: string,
    keySeparator: string,
    kvSeparator: string,
    replaceKeyWhitespace: boolean,
    keyWhitespaceReplacement: string,
    keyPrefix: string,
    string: string,
  ): Promise<{
    flatten: boolean;
    list_separator: string;
    key_separator: string;
    kv_separator: string;
    replace_key_whitespace: boolean;
    key_whitespace_replacement: string;
    key_prefix: string;
    string: string;
    matches: { [key: string]: string };
  }> {
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

    const promise = fetch('POST', qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'We were not able to run the JSON extraction. Please check your parameters.',
      );
    });

    return promise;
  },

  testRegexValidity(regex: string): Promise<{ is_valid: boolean }> {
    const encodedRegex = URI.encode(regex);
    const { url } = ApiRoutes.ToolsApiController.regexValidate(encodedRegex);
    const promise = fetch('GET', qualifyUrl(url));

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'Could not validate regular expression. Make sure that it is valid.',
      );
    });

    return promise;
  },
  urlWhiteListCheck(urlToCheck: string): Promise<{
    url: string;
    is_whitelisted: boolean;
  }> {
    const { url } = ApiRoutes.ToolsApiController.urlWhitelistCheck();
    const promise = fetch('POST', qualifyUrl(url), {
      url: urlToCheck,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`, 'Could not verify if the url is in the whitelist.');
    });

    return promise;
  },
  urlWhiteListGenerateRegex(
    urlTemplate: string,
    placeholder: string,
  ): Promise<{
    url_template: string;
    placeholder: string;
  }> {
    const { url } = ApiRoutes.ToolsApiController.urlWhitelistGenerateRegex();
    const promise = fetch('POST', qualifyUrl(url), {
      url_template: urlTemplate,
      placeholder,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`, 'Could not generate regex.');
    });

    return promise;
  },
  testRegex(
    regex: string,
    string: string,
  ): Promise<{
    regex: string;
    string: string;
    matched: boolean;
    match: {
      match: string;
    };
  }> {
    const { url } = ApiRoutes.ToolsApiController.regexTest();
    const promise = fetch('POST', qualifyUrl(url), {
      regex: regex,
      string: string,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'Could not try regular expression. Make sure that it is valid.',
      );
    });

    return promise;
  },
  testRegexReplace(
    regex: string,
    replacement: string,
    replaceAll: boolean,
    string: string,
  ): Promise<{
    regex: string;
    replacement: string;
    replace_all: boolean;
    string: string;
    matched: boolean;
    match: {
      match: string;
    };
  }> {
    const { url } = ApiRoutes.ToolsApiController.regexReplaceTest();
    const payload = {
      regex: regex,
      replacement: replacement,
      replace_all: replaceAll,
      string: string,
    };
    const promise = fetch('POST', qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'Could not try regular expression. Make sure that it is valid.',
      );
    });

    return promise;
  },
  testSplitAndIndex(
    splitBy: string,
    index: number,
    string: string,
  ): Promise<{
    split_by: string;
    index: number;
    string: string;
    successful: boolean;
    cut?: string;
  }> {
    const { url } = ApiRoutes.ToolsApiController.splitAndIndexTest();
    const payload = {
      split_by: splitBy,
      index: index,
      string: string,
    };

    const promise = fetch('POST', qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'We were not able to run the split and index extraction. Please check your parameters.',
      );
    });

    return promise;
  },
  testSubstring(
    beginIndex: number,
    endIndex: number,
    string: string,
  ): Promise<{
    start: number;
    end: number;
    string: string;
    successful: boolean;
    cut: string;
  }> {
    const { url } = ApiRoutes.ToolsApiController.substringTest();
    const payload = {
      start: beginIndex,
      end: endIndex,
      string: string,
    };

    const promise = fetch('POST', qualifyUrl(url), payload);

    promise.catch((errorThrown) => {
      UserNotification.error(
        `Details: ${errorThrown}`,
        'We were not able to run the substring extraction. Please check index boundaries.',
      );
    });

    return promise;
  },
  testContainsString(
    searchString: string,
    string: string,
  ): Promise<{
    search_string: string;
    string: string;
  }> {
    const { url } = ApiRoutes.ToolsApiController.containsStringTest();
    const promise = fetch('POST', qualifyUrl(url), {
      search_string: searchString,
      string: string,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`, 'Could not check if field contains the string');
    });

    return promise;
  },

  testLookupTable(
    lookupTableName: string,
    string: string,
  ): Promise<{
    lookup_table_name: string;
    string: string;
    error: string;
    empty: boolean;
    error_message: string;
    key: string;
    value: string;
  }> {
    const { url } = ApiRoutes.ToolsApiController.lookupTableTest();
    const promise = fetch('POST', qualifyUrl(url), {
      lookup_table_name: lookupTableName,
      string: string,
    });

    promise.catch((errorThrown) => {
      UserNotification.error(`Details: ${errorThrown}`, 'Could not check if lookup table translates the string');
    });

    return promise;
  },
};

export default ToolsStore;
