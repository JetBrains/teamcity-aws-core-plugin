

class BuildUtils {

    private ext

    BuildUtils(ext){
        this.ext = ext
    }

    def getPluginVersion(){
        if (!ext.providedPluginVersion) {
            return 'SNAPSHOT-' + new Date().format('yyyyMMddHHmm')
        }
        return ext.providedPluginVersion
    }
}