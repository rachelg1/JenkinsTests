def String A_END = ""
def String A_START = ""
def String template_file = ""

def A_DURATION = 3
node {
    deleteDir()
    //def buildStatus = "SUCCESS"
    git branch: 'main', url: 'https://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@github.com/rachelg1/Test.git'
    def currentStage = "";
    println env.scenarios
    def String[] scenarios_arr
    A_END = System.currentTimeSeconds()
    A_START = System.currentTimeSeconds() - (A_DURATION * 60)
    scenarios_arr = env.scenarios.split(',')
    scenarios_arr.each { entry ->
        try {
            catchError(message: "scenario : $entry - failed", buildResult: 'FAILURE', stageResult: 'FAILURE') {
                stage(entry) {
                    timestamps {
                        echo "$entry"
                        currentStage = entry;
                        if (TEST_TYPE == "BDD") {
                            runBehaveBDD(currentStage, A_START, A_END)
                        } else if (TEST_TYPE == "CRITICAL") {
                            template_file = (internal_user == 'appuser') ? 'test_template_appuser.txt ' : 'test_template.txt'
                            runBehaveCritical(currentStage, A_START, A_END, template_file)
                        } else {
                            echo "missing TEST_TYPE for this run,exit.."
                            currentBuild.result = 'FAILURE'
                            return
                        }
                        //runBehaveCritical(currentStage,A_START,A_END)
                        /**try{echo "$entry"
                         runBehave(entry)} catch(err){echo "build failed or not"
                         echo "Caught: ${err}"
                         //currentBuild.result = 'FAILURE'
                         buildStatus = 'FAILURE'}**/
                    }
                }
            }
        } catch (e) {
            echo 'This will run only if failed'

            // Since we're catching the exception in order to report on it,
            // we need to re-throw it, to ensure that the build is marked as failed
            throw e
        } finally {
            def currentResult = currentBuild.result ?: 'SUCCESS'
            def slackColor = (currentResult == 'SUCCESS') ? "#0fab65" : "#7d243a";
            wrap([$class: 'BuildUser']) {


                //slackSend channel: "@"+env.BUILD_USER_ID+",nft", message: "Hey - here is your critical dependencies tests results (<${env.BUILD_URL}|Open>) ", tokenCredentialId: 'slack-secret'
                slackSend channel: "@" + env.BUILD_USER_ID + ",nft", message: "${env.JOB_NAME} #${env.BUILD_NUMBER} completed as ${currentResult}  ${env.JOB_NAME}, click here to view (<${env.BUILD_URL}|Open>)", tokenCredentialId: 'slack-secret ', color: slackColor
            }
        }

    }

    //currentBuild.result = "SUCCESS"
    //echo BUILD_USER_ID


}

@NonCPS
def runBehaveCritical(scenario, A_START, A_END, template_file) {
    sh '''set +x
    
    echo "set up variable for the shell  "
    graphite="''' + graphite + '''"
    scenario="''' + scenario + '''"
    duration="''' + duration + '''"
    template_file="''' + template_file + '''"

pip3 install -r requirements.txt --no-index --find-links \'http://tlvmvnrepository.tlv.lpnet.com/artifactory/api/pypi/lp-pypi-virtual/simple/resilience_ansible/\' --trusted-host tlvmvnrepository.tlv.lpnet.com

echo "template file : ''' + template_file + '''"
python3 generate_feature_file.py $SERVICE_NAME ''' + template_file + '''

FEATURE_NAME=$(echo $JOB_NAME | awk -F "BDD_Resilience_" \'{print $2}\')

#FEATURE_NAME=$(echo $TEMP_FEATURE_NAME | awk -F "_Alpha" \'{print $1}\')
ENV="alpha_features"
allure_report=lp-nft-allure-results
mv generated_test.feature  "features/$ENV/"
FILE_PATH_BY_ENV="features/$ENV/generated_test.feature" 

#FILE_PATH_BY_ENV="generated_test.feature"
echo "file path by env : $FILE_PATH_BY_ENV" 
cat $FILE_PATH_BY_ENV

echo "start date for the whole test in readable format $(date -u) ts: $(date -u +%s)"
#TS_START=$(date -u +%s)


set -x
behave -i $FILE_PATH_BY_ENV -n "${scenario}" -D graphite=${graphite} -D duration=${duration} -f allure_behave.formatter:AllureFormatter -o $allure_report --no-capture --color
set +x
#echo "search for timestamp files"
#ls

TS_START=$(cat TS_DURING_START)
TS_END=$(cat TS_DURING_END)
#TS_END_OLD=$(date -u +%s) 
#TS_END_OLD=$(expr $TS_END - 40 )
#TS_END_OLD_READABLE= date -d @$TS_END
#TS_START_OLD=$(expr $TS_END - 120 );
#TS_START_READABLE=$(expr $TS_END - 120 );
#$(date -u)
echo "local start date of the duration ( TS_END - duration_defined ) ts: $TS_START"
echo "local stop date  ts: $TS_END"

#debug
#echo $A_START $A_END
#echo $TS_START $TS_START_OLD


echo "about to wait $wait_for_scoring second "
sleep $wait_for_scoring
echo "calling to  relyscoring"
SCORING_URL="va-a.relyscoring.int.liveperson.net:8080"
SCORING_PATH=""
SCORING_URL_PARAMS=""
if [[ "${STRATEGY}" == "a_b" ]];
then
  SCORING_PATH="lp_rely_scoring_compare_scenario_all_metrics"
else
  SCORING_PATH="lp_rely_scoring_compare_scenario_all_metrics_a_b_c"
fi

SCORING_URL_PARAMS="start_ts=$TS_START&end_ts=$TS_END&a_start_ts=''' + A_START + '''&a_end_ts=''' + A_END + '''"

echo "scoring for $SERVICE_NAME for metric name: $SCORING_PATH with SCORING_URL_PARAMS: $SCORING_URL_PARAMS"

set -x
response_code=$(curl -s --location --write-out '%{http_code}' --output response_body.json --request POST "http://$SCORING_URL/$SCORING_PATH?$SCORING_URL_PARAMS" \\
--form \'prometheus_group=prometheus_federations\' \\
--form \'environment=va-k8s\' \\
--form \'datacenter=Virginia\' \\
--form \'cluster=va-kube03\' \\
--form \'namespace=default\' \\
--form "lp_service=$actual_observed_service_name" \\
--form \'time_duration=1m\' \\
--form "metric=$metrics");

set +x

if [[ "$response_code" -ne 200 ]] ; then
  echo "Scoring response with non 200 status code,please conteat NFT team"
  exit 1
else 
  echo "Scoring return 200,proceed"
fi

cat response_body.json   | jq '.' ;
python3 print_results.py response_body.json ${STRATEGY}
result_status=$(cat response_body.json   | jq '.global_status') ;
echo "general status: ${result_status}" ;
#[[ "${result_status}" == \\"failure\\" ]] && exit 1
failed_status=\\"fail\\"
if [[ "${result_status}" == ${failed_status} ]]
then
  echo "general status is failure for this scenario - set as failed"
  exit 1
elif [[ ${result_status} == null ]]
then
  echo "failed to run scoring,exitig from this stage"
  exit 1
fi


'''
}

def runBehaveBDD(scenario, A_START, A_END) {
    sh '''set -x
echo "set up variable for the shell"
    graphite="''' + graphite + '''"
    scenario="''' + scenario + '''"
    duration="''' + duration + '''"


    pip3 install -r requirements.txt --no-index --find-links 'http://tlvmvnrepository.tlv.lpnet.com/artifactory/api/pypi/lp-pypi-virtual/simple/resilience_ansible/' --trusted-host tlvmvnrepository.tlv.lpnet.com

TEMP_FEATURE_NAME=$(echo $JOB_NAME | awk -F "BDD_Resilience_" '{print $2}')

FEATURE_NAME=$(echo $TEMP_FEATURE_NAME | awk -F "_Alpha" '{print $1}')
ENV="alpha_features"
allure_report=lp-nft-allure-results
FILE_PATH_BY_ENV="features/$ENV/$SERVICE_NAME/$SERVICE_NAME.feature"
echo "file path by env : $FILE_PATH_BY_ENV"



case "''' + scenario + '''" in
   "All scenarios")
      behave -i $FILE_PATH_BY_ENV -D duration=${duration} -D graphite=${graphite} -f allure_behave.formatter:AllureFormatter -o $allure_report --no-capture --color
      ;;
   "choose one scenario randomly")
      behave -i $FILE_PATH_BY_ENV -D random_one -D duration=${duration} -D graphite=${graphite} -f allure_behave.formatter:AllureFormatter -o $allure_report --no-capture --color
      ;;
   "All scenarios randomly order")
      behave -i $FILE_PATH_BY_ENV -D all_randomly_order -D duration=${duration} -D graphite=${graphite} -f allure_behave.formatter:AllureFormatter -o $allure_report --no-capture --color
      ;;
   *)
      behave -i $FILE_PATH_BY_ENV -n "${scenario}" -D duration=${duration} -D graphite=${graphite} -f allure_behave.formatter:AllureFormatter -o $allure_report --no-capture --color
      ;;
esac
'''

}
