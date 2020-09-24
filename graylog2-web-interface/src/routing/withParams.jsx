// @flow strict
import * as React from 'react';
import { useParams } from 'react-router-dom';

type InjectedProps = {| params: { [string]: string } |};

function withParams<Config>(
  Component: React.AbstractComponent<{| ...Config, ...InjectedProps |}>,
): React.AbstractComponent<Config> {
  return (props: Config) => {
    const params = useParams();

    return <Component {...props} params={params} />;
  };
}

export default withParams;
