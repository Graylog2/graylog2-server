// @flow strict
import * as React from 'react';

import DecoratorContext from './DecoratorContext';
import type { ValueRenderer, ValueRendererProps } from './ValueRenderer';

const pipelineFromDecorators = (decorators: Array<ValueRenderer>): ValueRenderer => {
  const Component: ValueRenderer = ({ value, ...rest }: ValueRendererProps) => decorators
    .reduce((prev, Cur) => <Cur {...rest} value={value}>{prev}</Cur>, null);

  return Component;
};

const DecoratedValue: ValueRenderer = (props: ValueRendererProps) => (
  <DecoratorContext.Consumer>
    {(decorators: Array<ValueRenderer>) => {
      const DecoratorPipeline = pipelineFromDecorators(decorators);

      return <DecoratorPipeline {...props} />;
    }}
  </DecoratorContext.Consumer>
);

export default DecoratedValue;
