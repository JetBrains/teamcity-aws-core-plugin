const path = require('path');
const nodeExternals = require('webpack-node-externals');
const getWebpackConfig = require('@jetbrains/teamcity-api/getWebpackConfig');
const LicenseChecker = require('@jetbrains/ring-ui-license-checker');

function createLicenseChecker(filename) {
    return new LicenseChecker({
        format: require('./third-party-licenses-json'),
        filename,
        exclude: [/@jetbrains/],
        surviveLicenseErrors: true,
    });
}

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
appConfig.plugins.push(createLicenseChecker('../../../../../appConfig-js-related-libraries.json'))

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
libConfig.plugins.push(createLicenseChecker('./libConfig-js-related-libraries.json'))

module.exports = [appConfig, libConfig];

