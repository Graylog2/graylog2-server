`ExternalLink` in a text:

```js
<p>
  Please read the <ExternalLink href="http://docs.graylog.org/">documentation</ExternalLink> to learn about the
  product.
</p>
```

`ExternalLink` with a different icon:

```js
<ExternalLink href="http://docs.graylog.org/" iconName="help">
  documentation
</ExternalLink>
```

`ExternalLink` without a `href` prop:

```js
<ExternalLink>documentation</ExternalLink>
```
