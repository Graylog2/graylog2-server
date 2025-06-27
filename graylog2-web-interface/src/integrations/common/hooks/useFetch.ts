import { useEffect, useState } from 'react';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

/* useFetch Custom Hook

Because of [Rules of Hooks](https://reactjs.org/docs/hooks-rules.html) we have to get fancy
in order to use fetch Promises and hooks.

PARAMETERS:
 - `url`: The url that will be fetched, can be set later with `setUrl` [required, but accepts `null`]
 - `setHook`: that you want to call after fetch [required]
 - `method`: RESTful HTTP method, [`'GET'` optional]
 - `options`: Object of data you'll send with fetch, [`{}` optional]
 - `callback` that will fire any part of your code you need after fetch [`() => {}` optional]

USE:
`const [status, setUrl] = useFetch(setNextHook, onFooBar);`
 - `status` will provide the current reducer state `{ loading, error, data }`
 - `setUrl` will be your hook to call as a submit handler within a subfunction, it needs the API route as a string:

EXAMPLES:
```
  const [fetchRegionsStatus] = useFetch(ApiRoutes.INTEGRATIONS.AWS.REGIONS, setRegions, 'GET');
  const [fetchStreamsStatus, setStreamsFetch] = useFetch(
    null,
    (response) => {
      setStreams(response);
      onSubmit();
    },
    'POST',
    { region: formData.awsCloudWatchAwsRegion ? formData.awsCloudWatchAwsRegion.value : '' },
  );

  const handleSubmit = () => {
    setStreamsFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.STREAMS);
  };
```
*/

interface ErrorType {
  additional?: {
    body?: {
      message: string;
    };
  };
  message?: string;
}

const parseError = (error: ErrorType) => {
  const fullError = error.additional && error.additional.body && error.additional.body.message;

  return fullError || error.message;
};

type Method = 'GET' | 'PUT' | 'POST' | 'DELETE';

type UseFetchType = (
  url: string,
  setHook: (data: any) => any,
  method: Method,
  option: any,
) => [config: { loading: boolean; error: any; data: any }, setFetchUrl: React.Dispatch<string>];

const useFetch: UseFetchType = (url, setHook = () => {}, method = 'GET', options = {}) => {
  const [fetchUrl, setFetchUrl] = useState<string>(url);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<any | null>(null);

  const qualifiedGetURL = fetchUrl
    ? qualifyUrl(
        `${fetchUrl}?eventHubName=${options.eventHubName}&connectionString=${options.connectionString}&consumerGroup=${options.consumerGroup}`,
      )
    : null;
  const qualifiedURL = fetchUrl ? qualifyUrl(fetchUrl) : null;

  useEffect(() => {
    let isFetchable = !!qualifiedURL;

    const fetchData = async () => {
      let fetcher = Promise.resolve();

      if (isFetchable && !data) {
        setLoading(true);

        if (method === 'GET') {
          fetcher = fetch(method, qualifiedGetURL);
        } else {
          fetcher = fetch(method, qualifiedURL, {
            ...options,
          });
        }

        fetcher
          .then((result) => {
            if (isFetchable) {
              setError(null);
              setData(result);
              setHook(result);
            }
          })
          .catch((err) => {
            if (isFetchable) {
              setData(null);
              setError(parseError(err));
            }
          })
          .finally(() => {
            if (isFetchable) {
              setLoading(false);
            }
          });
      }

      return fetcher;
    };

    fetchData();

    return () => {
      isFetchable = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [qualifiedURL, qualifiedGetURL]);

  return [{ loading, error, data }, setFetchUrl];
};

export default useFetch;
