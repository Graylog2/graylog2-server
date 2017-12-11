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
        if (dataLoaders.componentWillMount) {
          const dataLoader = this.props[dataLoaders.componentWillMount];
          if (dataLoader && typeof dataLoader === 'function') {
            dataLoader();
          }
        }
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
