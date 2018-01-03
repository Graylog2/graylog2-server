`ExternalLink` in a text:
```js
<p>Please read the <ExternalLink href="http://docs.graylog.org/">Graylog documentation</ExternalLink> to learn about the product.</p>
```

`ExternalLink` with a different icon:
```js
<ExternalLink href="http://docs.graylog.org/"
              iconClass="fa-external-link-square">
  Graylog documentation
</ExternalLink>
```

`ExternalLink` without a `href` prop:
```js
<ExternalLink>Graylog documentation</ExternalLink>
```