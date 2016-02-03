<html>
<head>
    <meta charset="UTF-8">
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

    .app {
        margin-bottom: 100px;
    }

    .mdl-layout__drawer-button > .material-icons {
        margin-top: 10px;
    }

    .mdl-layout {
        max-width: 1280px;
        margin-left: auto;
        margin-right: auto;
    }

    .page-content {
        width: 920px;
        margin-left: auto;
        margin-right: auto;
    }
    </style>
</head>

<body>

<div class="mdl-layout mdl-js-layout mdl-layout--fixed-header">
    <header class="mdl-layout__header">
        <div class="mdl-layout__header-row">
            <span class="mdl-layout-title"><g:message code="title"/></span>

            <div class="mdl-layout-spacer"></div>
            <nav class="mdl-navigation">
            </nav>
        </div>
    </header>

    <div class="mdl-layout__drawer">
        <span class="mdl-layout-title"><g:message code="title"/></span>
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

</body>
</html>