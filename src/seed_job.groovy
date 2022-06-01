import hudson.model.*
import hudson.security.*
import groovy.json.JsonSlurper

String config_file = readFileFromWorkspace("src/projects.json")

def InputJSON = new JsonSlurper().parseText(config_file);

InputJSON.projects.each {
    createPipeline(it)
}

def createPipeline(service) {
def newJobName = "${service.service_name}_test"

    
    pipelineJob(newJobName) {

        definition {
            cps {
                script(readFileFromWorkspace('src/pipeline.groovy'))
            }
        }

        parameters {
            wHideParameterDefinition {
                name("SERVICE_DETAILS")
                description("service details")
                defaultValue(service.service_name)
            }
        }
    }
}
