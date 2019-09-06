// @flow strict
import * as React from 'react';
import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

type Props = {
  children: React.Node,
};
const IfSearch = ({ children }: Props) => (
  <ViewTypeContext.Consumer>
    {viewType => ((viewType === View.Type.Search) ? children : null)}
  </ViewTypeContext.Consumer>
);

export default IfSearch;
