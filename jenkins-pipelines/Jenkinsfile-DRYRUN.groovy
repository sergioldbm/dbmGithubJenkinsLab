@Library('dbmaestro-library') _

/**********   LOCAL VARIABLES BEGIN   **********/
def varsPath = "jenkins-pipelines/vars.groovy"
def dbmJenkinsNode = "master"
/**********    LOCAL VARIABLES END    **********/

try{

  stage("Init"){
    node(dbmJenkinsNode){
      checkout scm
      //rootDir = pwd()
      def myvars = load varsPath
      //if ticket number comes from a Jenkins parameter
      if(env.TICKET){
        issueKey = env.TICKET
        feedbackToJira = false
      }
      //if ticket number comes from Jira
      else{
        issueKey = env.JIRA_ISSUE_KEY
        feedbackToJira = true
      }
      packageFolder = issueKey
    }
  }

  stage("Packaging") {
    node (dbmJenkinsNode) {
      cleanWs()
      //checkout whole repo if needed, to be able to see package folders
      checkout scm
      dbmCreateManifestFile(myvars.javaCmd, myvars.rootFolder, packageFolder, myvars.packageType)
      helpZipPackageFolder(myvars.rootFolder, packageFolder)
      dbmGetPackages(myvars.javaCmd, myvars.projectName, myvars.getPackagesFilePath, myvars.server, myvars.authType, myvars.useSSL, myvars.dbmCredentials)
      dbmPackage(myvars.javaCmd, myvars.projectName, myvars.rootFolder, packageFolder, myvars.server, myvars.authType, myvars.useSSL, myvars.dbmCredentials)
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
            jiraIssueAddLabel(issueKey, myvars.dryrunOkLabel)
        }
    }
  }

}
catch(e){
  if(feedbackToJira){
    withEnv(["JIRA_SITE=${myvars.jiraSite}"]) {
        jiraIssueAddLabel(issueKey, myvars.dryrunErrorLabel)
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
