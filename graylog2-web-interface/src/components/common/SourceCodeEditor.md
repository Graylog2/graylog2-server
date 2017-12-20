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
const annotations = [
  { row: 1, column: -1, text: 'oh noes!', type: 'error' },
  { row: 2, column: -1, text: 'easy!', type: 'warning' },
  { row: 3, column: -1, text: 'info!', type: 'info' },
];
<SourceCodeEditor id="editor-2"
                  annotations={annotations}
                  resizable={false}
                  readOnly
                  theme="dark"
                  value={code} />
```

Non-resizable editor without toolbar and with custom height and width:
```js
const code = `function foobar() {
  console.log('this is some source code!');
}
`;
<SourceCodeEditor id="editor-2"
                  resizable={false}
                  height={100}
                  toolbar={false}
                  width={400}
                  value={code} />
```