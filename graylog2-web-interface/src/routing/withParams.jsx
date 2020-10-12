// @flow strict
import * as React from 'react';
import { useParams } from 'react-router-dom';

type ParamsContext = { params: { [string]: ?string } };

function withParams<Props: ParamsContext & { ... }, ComponentType: React$ComponentType<Props>>(
  Component: ComponentType,
): React$ComponentType<$Diff<React$ElementConfig<ComponentType>, ParamsContext>> {
  return (props) => {
    const params = useParams();

    return <Component {...props} params={params} />;
  };
}

export default withParams;
