const path = require('path')
const getWebpackConfig = require('@jetbrains/teamcity-api/getWebpackConfig');

module.exports = getWebpackConfig({
    srcPath: path.join(__dirname, './src'),
    // outputPath: path.resolve(__dirname, '../aws-core-server/src/main/resources/buildServerResources'),
    outputPath: path.resolve(__dirname, 'build'),
    entry: './src/index.tsx',
    useTypeScript: true,
});