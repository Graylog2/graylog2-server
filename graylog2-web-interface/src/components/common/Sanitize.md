Renders sanitized HTML inside a `<span>`. Use it anywhere you would otherwise reach for `dangerouslySetInnerHTML` — the component wraps `DOMPurify.sanitize` and owns the single `react/no-danger` eslint-disable.

Safe HTML passes through untouched:

```js
<Sanitize html="<b>bold</b> and <i>italic</i>" />
```

Malicious input is stripped:

```js
<Sanitize html={'hello<script>alert(1)</script>world'} />
```

Nullish input renders an empty span:

```js
<Sanitize html={null} />
```

Any standard HTML attribute is forwarded to the rendered span:

```js
<Sanitize html="feed title" className="feed-title" title="Open article" />
```

Pass a `config` object for non-default `DOMPurify` profiles (e.g. SVG content):

```js
<Sanitize
  html='<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"><circle cx="12" cy="12" r="10" fill="currentColor" /></svg>'
  config={{ USE_PROFILES: { svg: true } }}
/>
```
