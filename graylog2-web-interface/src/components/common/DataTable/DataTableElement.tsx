import React from 'react';

type DataTableElementProps = {
  /** Element to be formatted. */
  element?: any;
  /**
   * Formatter function. It expects to receive the `element`, and `index` as arguments and
   * returns an element to be rendered.
   */
  formatter: (...args: any[]) => void;
  /** Element index. */
  index?: number;
};

/**
 * Component used to encapsulate each header or row inside a `DataTable`. You probably
 * should not use this component directly, but through `DataTable`. Look at the `DataTable`
 * section for a usage example.
 */
class DataTableElement extends React.Component<DataTableElementProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    element: undefined,
    index: undefined,
  };

  render() {
    const { formatter, element, index } = this.props;

    return formatter(element, index);
  }
}

export default DataTableElement;
