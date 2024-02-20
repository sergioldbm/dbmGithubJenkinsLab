def call(String rootFolder, String packageFolder){
    def path = "${rootFolder}\\${packageFolder}\\*"
    helpMsgbox("Zipping package folder content (${path})")
    echo "Zipping package folder content (${path})"
    echo " Destination Path ${rootFolder}\\${packageFolder}.zip"
    powershell(returnStdout: true, script: "Compress-Archive -Path ${path} -DestinationPath ${rootFolder}\\${packageFolder}.zip -Force")
}