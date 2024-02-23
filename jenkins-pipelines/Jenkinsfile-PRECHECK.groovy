@Library('dbmaestro-library') _

/**********   LOCAL VARIABLES BEGIN   **********/
def varsPath = "jenkins-pipelines/vars.groovy"
def dbmJenkinsNode = "master"
/**********    LOCAL VARIABLES END    **********/

try{
  stage("Init"){
    node (dbmJenkinsNode) {
      checkout scm
      echo "NODE_NAME = ${env.NODE_NAME}"
      //rootDir = pwd()
      feedbackToJira = false
      echo "$feedbackToJira"
      myvars = load varsPath
      //if ticket number comes from a Jenkins parameter9
      if(env.TICKET){
        issueKey = env.TICKET
        echo "$issueKey"
        feedbackToJira = false
        echo "$feedbackToJira"
      }
      //if ticket number comes from Jira
      else{
        issueKey = env.JIRA_ISSUE_KEY
        echo "$issueKey"
        feedbackToJira = true
        echo "$feedbackToJira"
      }
      packageFolder = issueKey
    }
  }

  stage("Packaging") {
    node (dbmJenkinsNode) {
      cleanWs()
      //checkout whole repo if needed, to be able to see package folders
      checkout scm
      dbmGetPackages(myvars.javaCmd, myvars.projectName, myvars.getPackagesFilePath, myvars.server, myvars.authType, myvars.useSSL, myvars.dbmCredentials)
      packageExists = dbmPackageExists(packageFolder, myvars.getPackagesFilePath)
      helpMsgbox("Package exists?: '${packageExists}'")
      if(packageExists){
        helpMsgbox("Don't create package in DBmaestro, it already exists")
      }
      else{
        helpMsgbox("Upload to DBmaestro")
        dbmCreateManifestFile(myvars.javaCmd, myvars.rootFolder, packageFolder, myvars.packageType)
        helpZipPackageFolder(myvars.rootFolder, packageFolder)
        dbmPackage(myvars.javaCmd, myvars.projectName, myvars.rootFolder, packageFolder, myvars.server, myvars.authType, myvars.useSSL, myvars.dbmCredentials)
      }
    }
  }

  //DRY RUN ENV (PRECHECK)
  stage("DryRun"){
    node (dbmJenkinsNode) {
      dbmPreCheck(myvars.javaCmd, myvars.projectName, packageFolder, myvars.server, myvars.authType, myvars.useSSL, myvars.dbmCredentials)
    }
  }

  if(feedbackToJira){
    stage("Update Jira Issue"){
        withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
          def comment = [ body: "${BUILD_URL}console" ]
          jiraIssueAddLabel(issueKey, myvars.precheckOkLabel)
        }
    }
  }

}
catch(e){
  if(feedbackToJira){
    withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
      def comment = [ body: "${BUILD_URL}console" ]
      jiraIssueAddLabel(issueKey, myvars.precheckErrorLabel)
    }
  }
  throw e
}
finally{
  if(feedbackToJira){
    withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
        def comment = [ body: "${BUILD_URL}console" ]
        jiraIssueAddComment(issueKey, comment)
    }
  }
}
