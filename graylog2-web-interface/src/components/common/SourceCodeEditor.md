Light-themed editor:
```js
const createReactClass = require('create-react-class');

const LightSourceEditor = createReactClass({
  getInitialState() {
    return {
      code: `function foobar() {
  console.log('this is some source code!');
}
`,
    };
  },
  
  handleChange(nextValue) {
    this.setState({ code: nextValue });
  },
  
  render() {
    const { code } = this.state;
    return (
      <div>
        <SourceCodeEditor id="editor-1"
                          theme="light"
                          value={code}
                          onChange={this.handleChange} />
        <p>Preview:</p>
        <pre>{code}</pre>
      </div>
    );
  },
});

<LightSourceEditor />
```

Read-only dark-themed editor:
```js
const code = `function foobar() {
  console.log('this is some source code!');
}
`;
<SourceCodeEditor id="editor-2"
                  readOnly
                  theme="dark"
                  value={code} />
```