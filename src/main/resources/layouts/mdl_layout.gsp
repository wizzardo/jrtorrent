<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:layoutTitle default="Default Title"/></title>
    <link rel="stylesheet" href="https://storage.googleapis.com/code.getmdl.io/1.0.6/material.indigo-pink.min.css">
    <script src="https://storage.googleapis.com/code.getmdl.io/1.0.6/material.min.js"></script>
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
    <link href='https://fonts.googleapis.com/css?family=Roboto:400,700&subset=latin,cyrillic' rel='stylesheet'
          type='text/css'>
    <g:layoutHead/>

    <style>
    body {
        font-family: 'Roboto', sans-serif;
    }

    .page-content {
        margin: 1em;
    }

    .mdl-layout__drawer-button > .material-icons {
        margin-top: 10px;
    }
    </style>
</head>

<body>

<div class="mdl-layout mdl-js-layout mdl-layout--fixed-header">
    <header class="mdl-layout__header">
        <div class="mdl-layout__header-row">
            <span class="mdl-layout-title">${title ?: 'Title'}</span>

            <div class="mdl-layout-spacer"></div>
            <nav class="mdl-navigation">
                <add_button>add</add_button>
            </nav>
        </div>
    </header>

    <div class="mdl-layout__drawer">
        <span class="mdl-layout-title">${title ?: 'Title'}</span>
        <nav class="mdl-navigation">
            <a class="mdl-navigation__link" href="">Link</a>
            <a class="mdl-navigation__link" href="">Link</a>
            <a class="mdl-navigation__link" href="">Link</a>
            <a class="mdl-navigation__link" href="">Link</a>
        </nav>
    </div>
    <main class="mdl-layout__content">
        <div class="page-content">
            <g:layoutBody/>
        </div>
    </main>
</div>

<script src="/static/js/tags/add_button.tag" type="riot/tag"></script>
<script>
    riot.mount('add_button');
</script>
</body>
</html>