import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

properties(
    [
        parameters(
            [
                booleanParam(name: 'AWS', defaultValue: true, description: 'Make Amazon Machine Image/Not?'),
                string(name: 'LLM_STUDIO_VERSION')
            ]
        )
    ]
)

node('docker') {
    stage('Init') {
        cleanWs()
        currentBuild.displayName = "#${BUILD_NUMBER} - Rel:${LLM_STUDIO_VERSION}"
        checkout scm
        sh('ls -al')
    }

    stage('Build Images') {
        try {
            docker.image('harbor.h2o.ai/opsh2oai/h2oai-packer-build:2').inside {
                parallel([
                        "AWS Ubuntu 20.04": {
                            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'jenkins-full-aws-creds'],
                                             string(credentialsId: "AWS_MARKETPLACE_VPC", variable: "aws_vpc_id"),
                                             string(credentialsId: "AWS_MARKETPLACE_OWNERS", variable: "aws_owners"),
                                             string(credentialsId: "AWS_MARKETPLACE_SUBNET", variable: "aws_subnet_id"),
                                             string(credentialsId: "AWS_MARKETPLACE_SG", variable: "aws_security_group_id")]) {
                                dir('jenkins') {
                                    if (params.aws.toBoolean()) {
                                        sh("packer build \
                                        -var 'aws_access_key=$AWS_ACCESS_KEY_ID' \
                                        -var 'aws_secret_key=$AWS_SECRET_ACCESS_KEY' \
                                        -var 'llm_studio_version=${LLM_STUDIO_VERSION}' \
                                        -var 'aws_region=us-east-1' \
                                        -var 'aws_vpc_id=$aws_vpc_id' \
                                        -var 'aws_owners=$aws_owners' \
                                        -var 'aws_subnet_id=$aws_subnet_id' \
                                        -var 'aws_security_group_id=$aws_security_group_id' \
                                        llm-studio-aws.json"
                                        )
                                        archiveArtifacts artifacts: '*-image-info.json'
                                    }else {
                                        Utils.markStageSkippedForConditional('AWS Ubuntu 20.04')
                                    }
                                }
                            }
                        },
                ])
            }
        } finally {
            cleanWs()
        }
    }
}


withCredentials(){
   dir('jenkins'){
   } 
}