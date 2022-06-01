import hudson.model.*
import hudson.security.*
import groovy.json.JsonSlurper

String config_file = readFileFromWorkspace("src/projects.json")

def InputJSON = new JsonSlurper().parseText(config_file);

InputJSON.projects.each {
    if (it.group != null && it.service_name != null && it.enable == true) {
        createPipeline(it)
    }
    return true
}

def createPipeline(service) {

    def addr = "https://toga.int.liveperson.net/api/groups/${service.group}"
    def conn = addr.toURL().openConnection()
    def feed = conn.content.text
    def results = new JsonSlurper().parseText(feed.toString())

    def newJobName = "NFT_${service.service_name}_critical_pipeline"
    if (!service.in_container_user?.trim()) {
        service.in_container_user = "root"
    }
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
                defaultValue(service)
            }
        }
    }
}
