import { defineConfig, transformWithOxc  } from 'vite';
import react from '@vitejs/plugin-react'

const transformJsxInJs = () => ({
    name: "transform-jsx-in-js",
    enforce: "pre",
    async transform(code, id) {
        if (!id.match(/.*\.js$/)) {
            return null;
        }

        return await transformWithOxc(code, id, {
            lang: "jsx",
        });
    },
});

export default defineConfig({
    plugins: [
        react(),
        transformJsxInJs(),
    ],
    legacy: {
        inconsistentCjsInterop: true,
    }
})