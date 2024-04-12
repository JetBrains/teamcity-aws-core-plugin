const path = require('path');
const nodeExternals = require('webpack-node-externals');

const getWebpackConfig = require('@jetbrains/teamcity-api/getWebpackConfig');

const appConfig = getWebpackConfig({
  srcPath: path.join(__dirname, './src'),
  outputPath: path.resolve(
    __dirname,
    '../aws-core-webapp/src/main/webapp/plugins/aws-core-plugin'
  ),
  entry: './src/index.tsx',
  useTypeScript: true,
})();
appConfig.name = 'app';

const libConfig = getWebpackConfig({
  srcPath: path.join(__dirname, './src'),
  outputPath: path.resolve(__dirname, 'dist'),
  entry: './src/components.tsx',
  useTypeScript: true,
})();

libConfig.name = 'lib';
libConfig.devtool = 'source-map';
libConfig.output.library = {
  name: 'awsConnectionUiComponents',
  type: 'umd',
};
libConfig.output.filename = 'components.js';
libConfig.externals = [nodeExternals()];
libConfig.target = 'node';

module.exports = [appConfig, libConfig];
