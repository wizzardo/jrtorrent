import babel from 'rollup-plugin-babel';
import resolve from 'rollup-plugin-node-resolve';
import commonjs from 'rollup-plugin-commonjs';
import json from 'rollup-plugin-json';
import replace from 'rollup-plugin-replace';
import styles from "rollup-plugin-styles";
import {terser} from "rollup-plugin-terser";
import fs from "fs-extra";

const env = process.env.NODE_ENV || 'production';
// const env = 'development';
const isProd = env === 'production';
const isDev = !isProd;
const randomHexString = (length) => {
    let text = "";
    const possible = "abcdef0123456789";
    for (let i = 0; i < length; i++)
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    return text;
};

const version = randomHexString(8);

if (!fs.existsSync('build'))
    fs.mkdirSync('build');

const prepareHtml = () => {
    let htmlTemplate = fs.readFileSync('src/template.html', {encoding: 'utf8', flag: 'r'});
    htmlTemplate = htmlTemplate.replace('<% preact.headEnd %>', `<link  href="static/${version}/bundle.css" rel="stylesheet">`)
    htmlTemplate = htmlTemplate.replace('<% preact.bodyEnd %>', `<script defer src="static/${version}/index.js"></script>`)

    fs.writeFileSync('build/index.html', htmlTemplate, {encoding: "utf8",})
}

const copyAssets = () => fs.copySync('src/assets', 'build/assets');

prepareHtml();
copyAssets();

export default {
    input: 'src/index.js',
    output: [
        {
            dir: `build/static/${version}`,
            // format: 'system',
            format: 'iife',
            sourcemap: isProd,
            assetFileNames: "[name][extname]",
        },
    ],
    treeshake: isProd,
    inlineDynamicImports: isDev || false, // true = disabling code splitting to chunks
    // experimentalOptimizeChunks: true,
    // chunkGroupingSize: 10240,
    perf: false,
    watch: {
        chokidar: true,
        include: 'src/**',
        exclude: ['node_modules/**'],
    },
    plugins: [
        styles({
            mode: ["extract", "bundle.css"],
            // output: `bundle.css`,
        }),
        resolve({
            browser: true,
        }),
        json(),
        // alias({
        //     entries: [
        //         { find: 'react', replacement: 'preact/compat' },
        //         { find: 'react-dom', replacement: 'preact/compat' }
        //     ]
        // }),
        commonjs({
            include: [
                'node_modules/**',
            ],
            exclude: [
                'node_modules/process-es6/**',
            ],
            namedExports: {
                'node_modules/react/index.js': ['Children', 'Component', 'PropTypes', 'createElement', 'PureComponent',
                    'useLayoutEffect', 'useEffect', 'useState', 'useMemo', 'useContext', 'useReducer', 'useRef'],
                'node_modules/react-is/index.js': ['isValidElementType', 'isContextConsumer'],
                'node_modules/react-redux/node_modules/react-is/index.js': ['isValidElementType', 'isContextConsumer'],
                'node_modules/react-dom/index.js': ['render', 'unstable_batchedUpdates'],
                // 'node_modules/chart.js/src/chart.js': ['Chart'],
                'node_modules/chart.js/dist/Chart.js': ['Chart'],
                'node_modules/react-sparklines/build/index.js': ['Sparklines', 'SparklinesLine'],
            },
        }),
        babel({
            // exclude: 'node_modules/**',
            exclude: [
                'node_modules/!(' +
                'preact|preact-compat|react-redux|react-ui-basics|prop-types' +
                ')/**',
            ],
            babelrc: false,
            presets: [
                // ["@babel/env", {"modules": false}],
                ["@babel/react", {"pragma": "ReactCreateElement"}],
            ],
            plugins: [
                "@babel/external-helpers",
                // Stage 0
                "@babel/plugin-proposal-function-bind",

                // Stage 1
                "@babel/plugin-proposal-export-default-from",
                "@babel/plugin-proposal-logical-assignment-operators",
                ["@babel/plugin-proposal-optional-chaining", {"loose": false}],
                ["@babel/plugin-proposal-pipeline-operator", {"proposal": "minimal"}],
                ["@babel/plugin-proposal-nullish-coalescing-operator", {"loose": false}],
                "@babel/plugin-proposal-do-expressions",

                ["@babel/plugin-proposal-private-property-in-object", {"loose": true}],
                ["@babel/plugin-proposal-class-properties", {"loose": true}],
                "@babel/plugin-syntax-dynamic-import",

                "transform-react-remove-prop-types",
                ["babel-plugin-jsx-pragmatic", {module: "react-ui-basics/ReactCreateElement", import: "ReactCreateElement"}],
                ["module-resolver", {
                    "root": ["./src"],
                    "alias": {
                        'react': 'preact/compat',
                        'react-dom': 'preact/compat',
                    }
                }],
            ],
        }),
        (isProd && terser()),
        replace({
            'process.env.NODE_ENV': JSON.stringify(env),
        }),
    ],
};