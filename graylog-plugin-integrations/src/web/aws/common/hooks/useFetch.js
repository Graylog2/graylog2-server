import { useContext, useEffect, useState } from 'react';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import { FormDataContext } from '../../context/FormData';
import awsAuth from '../awsAuth';

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

const parseError = (error) => {
  const fullError = error.additional && error.additional.body && error.additional.body.message;
  return fullError || error.message;
};

const useFetch = (url, setHook = () => {}, method = 'GET', options = {}) => {
  const { formData } = useContext(FormDataContext);
  const [fetchUrl, setFetchUrl] = useState(url);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);
  const { key, secret } = awsAuth(formData);
  const qualifiedURL = fetchUrl ? URLUtils.qualifyUrl(fetchUrl) : null;

  useEffect(() => {
    let isFetchable = !!qualifiedURL;

    const fetchData = async () => {
      let fetcher = Promise.resolve();

      if (isFetchable && !data) {
        setLoading(true);

        const {
          awsCloudWatchAssumeARN = { value: undefined },
          awsEndpointCloudWatch = { value: undefined },
          awsEndpointIAM = { value: undefined },
          awsEndpointDynamoDB = { value: undefined },
          awsEndpointKinesis = { value: undefined },
        } = formData;

        if (method === 'GET') {
          fetcher = fetch(method, qualifiedURL);
        } else {
          fetcher = fetch(method, qualifiedURL, {
            aws_access_key_id: key,
            aws_secret_access_key: secret,
            assume_role_arn: awsCloudWatchAssumeARN.value,
            cloudwatch_endpoint: awsEndpointCloudWatch.value,
            dynamodb_endpoint: awsEndpointDynamoDB.value,
            iam_endpoint: awsEndpointIAM.value,
            kinesis_endpoint: awsEndpointKinesis.value,
            ...options,
          });
        }

        fetcher.then((result) => {
          setError(null);
          setData(result);
          setHook(result);
        }).catch((err) => {
          setData(null);
          setError(parseError(err));
        }).finally(() => {
          setLoading(false);
        });
      }

      return fetcher;
    };

    fetchData();

    return () => {
      isFetchable = false;
    };
  }, [qualifiedURL]);

  return [{ loading, error, data }, setFetchUrl];
};

export default useFetch;
