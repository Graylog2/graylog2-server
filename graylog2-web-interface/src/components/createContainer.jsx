import React from 'react';
import lodash from 'lodash';
import { connect } from 'react-redux';

const createContainer = (mapStateToProps, mapDispatchToProps) => {
  return (Component, dataLoaders = {}) => {
    const allDataLoaders = Object.values(dataLoaders);

    class SimpleContainer extends React.Component {
      static filterProps(props) {
        const filteredProps = {};
        lodash.forEach(props, (propValue, propKey) => {
          if (!allDataLoaders.includes(propKey)) {
            filteredProps[propKey] = propValue;
          }
        });
        return filteredProps;
      }

      componentWillMount() {
        const componentWillMountDataLoaders = dataLoaders.componentWillMount;
        if (!componentWillMountDataLoaders) {
          return;
        }
        const dataLoaderFunctions = Array.isArray(componentWillMountDataLoaders) ? componentWillMountDataLoaders : [componentWillMountDataLoaders];
        dataLoaderFunctions.forEach((dataLoaderFunctionName) => {
          const dataLoader = this.props[dataLoaderFunctionName];
          if (dataLoader) {
            if (typeof dataLoader === 'function') {
              dataLoader();
            }
          }
        });
      }

      render() {
        const props = SimpleContainer.filterProps(this.props);
        return <Component {...props} />;
      }
    }

    return connect(
      mapStateToProps,
      mapDispatchToProps,
    )(SimpleContainer);
  };
};

export default createContainer;
