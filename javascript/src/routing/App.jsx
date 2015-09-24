import React from 'react';
import Navigation from 'components/navigation/Navigation';

const App = React.createClass({
  render() {
    return (
      <div>
        <Navigation requestPath="/" fullName="John Doe" loginName="johndoe"/>
        {this.props.children}
      </div>
    );
  },
});

export default App;
