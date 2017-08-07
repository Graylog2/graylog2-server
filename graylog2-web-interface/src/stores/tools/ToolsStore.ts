import ApiRoutes = require('routing/ApiRoutes');
const URLUtils = require('util/URLUtils');
const UserNotification = require('util/UserNotification');
const fetch = require('logic/rest/FetchProvider').default;

const ToolsStore = {
    testNaturalDate(text: string): Promise<string[]> {
        const url = ApiRoutes.ToolsApiController.naturalDateTest(text).url;
        const promise = fetch('GET', URLUtils.qualifyUrl(url));

        promise.catch((errorThrown) => {
            if (errorThrown.additional.status !== 422) {
                UserNotification.error("Loading keyword preview failed with status: " + errorThrown,
                    "Could not load keyword preview");
            }
        });

        return promise;
    },
    testGrok(pattern: string, namedCapturesOnly: boolean, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.grokTest().url;
        const promise = fetch('POST', URLUtils.qualifyUrl(url), {pattern: pattern, string: string, named_captures_only: namedCapturesOnly});

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
                'We were not able to run the grok extraction. Please check your parameters.');
        });

        return promise;
    },
    testJSON(flatten: boolean, listSeparator: string, keySeparator: string, kvSeparator: string, replaceKeyWhitespace: boolean, keyWhitespaceReplacement: string, keyPrefix: string, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.jsonTest().url;
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
            UserNotification.error('Details: ' + errorThrown,
                'We were not able to run the JSON extraction. Please check your parameters.');
        });

        return promise;
    },
    testRegex(regex: string, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.regexTest().url;
        const promise = fetch('POST', URLUtils.qualifyUrl(url), {regex: regex, string: string});

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
                'Could not try regular expression. Make sure that it is valid.');
        });

        return promise;
    },
    testRegexReplace(regex: string, replacement: string, replaceAll: boolean, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.regexReplaceTest().url;
        const payload = {
            regex: regex,
            replacement: replacement,
            replace_all: replaceAll,
            string: string
        };
        const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
                'Could not try regular expression. Make sure that it is valid.');
        });

        return promise;
    },
    testSplitAndIndex(splitBy: string, index: number, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.splitAndIndexTest().url;
        const payload = {
            split_by: splitBy,
            index: index,
            string: string,
        };

        const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
                'We were not able to run the split and index extraction. Please check your parameters.');
        });

        return promise;
    },
    testSubstring(beginIndex: number, endIndex: number, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.substringTest().url;
        const payload = {
            start: beginIndex,
            end: endIndex,
            string: string,
        };

        const promise = fetch('POST', URLUtils.qualifyUrl(url), payload);

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
                'We were not able to run the substring extraction. Please check index boundaries.');
        });

        return promise;
    },
    testContainsString(searchString: string, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.containsStringTest().url;
        const promise = fetch('POST', URLUtils.qualifyUrl(url), { search_string: searchString, string: string });

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown,
              'Could not check if field contains the string');
        });

        return promise;
    },

    testLookupTable(lookupTableName: string, string: string): Promise<Object> {
        const url = ApiRoutes.ToolsApiController.lookupTableTest().url;
        const promise = fetch('POST', URLUtils.qualifyUrl(url), { lookup_table_name: lookupTableName, string: string });

        promise.catch((errorThrown) => {
            UserNotification.error('Details: ' + errorThrown, 'Could not check if lookup table translates the string');
        });

        return promise;
    },
};

export = ToolsStore;
