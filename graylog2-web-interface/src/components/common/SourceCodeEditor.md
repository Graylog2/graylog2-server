Light-themed editor:
```js
class MarkdownSourceEditor extends React.Component {
  constructor(props) {
    this.state = {
      code: `## Markdown text

- This is an example of a source text editor
- The code we write is written in Markdown
`,
    };
    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(nextValue) {
    this.setState({ code: nextValue });
  }

  render() {
    const { code } = this.state;
    return (
      <div>
        <SourceCodeEditor id="editor-1"
                          mode="markdown"
                          theme="light"
                          value={code}
                          onChange={this.handleChange} />
        <p>Preview:</p>
        <pre>{code}</pre>
      </div>
    );
  }
}

<MarkdownSourceEditor />
```

Read-only dark-themed editor:
```js
class TextSourceEditor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      code: `function foobar() {
  console.log('this is some source code!');
}
`,
    };
    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(nextValue) {
    this.setState({ code: nextValue });
  };

  render() {
    const { code } = this.state;
    const annotations = [
      { row: 1, column: -1, text: 'oh noes!', type: 'error' },
      { row: 2, column: -1, text: 'easy!', type: 'warning' },
      { row: 3, column: -1, text: 'info!', type: 'info' },
    ];
    return (
        <SourceCodeEditor id="editor-2"
                          annotations={annotations}
                          resizable={false}
                          readOnly
                          theme="dark"
                          value={code} />
    );
  }
}

<TextSourceEditor />
```

Non-resizable editor without toolbar and with custom height and width:
```js
class JsonSourceEditor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      code: `{
  "key": "value",
  "foo": [
    "bar",
    "baz"
  ]
}
`,
    };
    this.handleChange = this.handleChange.bind(this);
  }

  handleChange(nextValue) {
    this.setState({ code: nextValue });
  };

  render() {
    const { code } = this.state;
    return (
      <SourceCodeEditor id="editor-2"
                      height={100}
                      mode="json"
                      onChange={this.handleChange}
                      resizable={false}
                      toolbar={false}
                      width={400}
                      value={code} />
      );
  }
}

<JsonSourceEditor />
```