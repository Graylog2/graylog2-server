# Short Markdown Example

## Introduction
Markdown should be allowed. Quotes and certain chars are ok too "'`@:

Standard entities should be unescaped: &#34; &quot; &#39; &apos; &#8220; &ldquo; &#8221; &rdquo; &#8216; &lsquo; &#8217; &rsquo; &#8249; &lsaquo; &#8250; &rsaquo; &#171; &laquo; &#187; &raquo;

### List
- Item 1
- Item 2
- Item 3

<script src="app.js"></script>
<script async src="app.js"></script>
<script defer src="app.js"></script>
<script>
    document.addEventListener("DOMContentLoaded", function() {
        console.log("Test!");
    });
    
    function showMessage() {
        alert('Hello, this is a simple JavaScript alert!');
    }
    
    function validateForm() {
        let name = document.getElementById("name").value;
        if (name === "") {
            alert("Name cannot be empty!");
            return false;
        }
        return true;
    }
</script>

<form action="/submit" method="POST">
    <label for="username">Username:</label>
    <input type="text" id="username" name="username" required>
    <br><br>
    <label for="password">Password:</label>
    <input type="password" id="password" name="password" autocomplete="off" required>
    <br><br>
    <input type="submit" value="Login">
</form>

<button type="button" onclick="function()">Click Securely</button>

<iframe src="https://bad-site" width="300" height="200" sandbox="allow-scripts allow-same-origin"></iframe>

<a href="https://example.com" target="_blank" rel="noopener noreferrer">Visit Example</a>

<footer>
    <p>Footer</p>
</footer>
