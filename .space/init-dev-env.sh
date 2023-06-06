mkdir teamcity
curl --output ./teamcity/TeamCity.tar.gz https://download-cdn.jetbrains.com/teamcity/TeamCity-2023.05.tar.gz
mkdir teamcity/TeamCity
tar -xvf ./teamcity/TeamCity.tar.gz -C ./teamcity/TeamCity --strip-components=1