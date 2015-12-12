<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><g:layoutTitle default="Default Title"/></title>
    <link rel="stylesheet" href="https://storage.googleapis.com/code.getmdl.io/1.0.6/material.indigo-pink.min.css">
    <script src="https://storage.googleapis.com/code.getmdl.io/1.0.6/material.min.js"></script>
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
    <g:layoutHead/>

    <style>
    .page-content {
        margin: 1em;
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
                <button class="mdl-button mdl-js-button mdl-button--accent" style="font-weight: bold">
                    Add
                </button>
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

</body>
</html>