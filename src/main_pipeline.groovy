node (){
    def currentStage = "";
    def String[] scenarios_arr
    def failed_scenarios = []
    scenarios_arr = "s1,s2,s3".split(',')
    scenarios_arr.each { entry ->

        //catchError(message: "scenario : $entry - failed", buildResult: 'FAILURE', stageResult: 'FAILURE') {
        stage(entry) {
            try {
                echo "$entry"
                currentStage = entry
                if (entry == "s2")
                    throw new Exception("hello")
            }
            //}
            catch (e) {
                echo 'scenario : ' + entry + ' - failed: ' + e
                failed_scenarios.add(entry)
                // Since we're catching the exception in order to report on it,
                // we need to re-throw it, to ensure that the build is marked as failed
                //throw e
            }
        }

    }

    if (failed_scenarios.size() != 0) {
        echo 'failed_scenarios: ' + "[\"${failed_scenarios.join('", "')}\"]"
        currentBuild.result = 'FAILURE'
    }

}

def runBehaveBDD(scenario, A_START, A_END) {
    sh '''set -x
echo "set up variable for the shell"
    scenario="''' + scenario + '''"
'''

}
