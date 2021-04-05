export default {
    webpack(config, env, helpers, options) {
        const [ css ] = helpers.getLoadersByName(config, 'css-loader');
        css.loader.options.modules = false;
    }
};