import os, requests, json, warnings
from datetime import datetime
from urllib3.exceptions import InsecureRequestWarning

# Suppress only the single InsecureRequestWarning from urllib3
warnings.simplefilter('ignore', InsecureRequestWarning)

SERVER_NAME = "PJD Jenkins"
SERVER_URL = (os.environ["SERVER_URL"])
USER = (os.environ["USER"])
API_TOKEN = (os.environ["API_TOKEN"])
METRICS_KEY = (os.environ["METRICS_KEY"])
WEBHOOK_URL = (os.environ["WEBHOOK_URL"])

def send_message():
    widgets = []

    for i in range(len(BUILD_NUMBER)):
        time_str, date_str, elapsed = convert_timestamp(BUILD_TIMESTAMP[i])

        widget = {
            "widgets": [
                {
                    "decoratedText": {
                        "topLabel": f"<b>Build Name</b> ",
                        "text": f"<font color=\"#FF0000\"><b>{BUILD_NAME[i]}</b></font>"
                    }
                },
                {
                    "decoratedText": {
                        "topLabel": f"<b>Build Label</b> ",
                        "text": f"<font color=\"#808080\">{BUILD_PATH[i]}</font>",
                    }
                },
                {
                    "buttonList": {
                        "buttons": [
                            {
                                "text": f"🔴  Check Build #{BUILD_NUMBER[i]}",
                                "onClick": {
                                    "openLink": {
                                        "url": BUILD_URL[i]
                                    }
                                }
                            }
                        ]
                    }
                },
                {
                    "decoratedText": {
                        "topLabel": f"<b>Owner</b> ",
                        "text": f"<b><font color=\"#FFA500\">{OWNER[i]}</font></b>" if OWNER[i] != "N/A" else f"<b><font color=\"#FFA500\">{OWNER[i]}</font></b>",
                        "bottomLabel": f"<font color=\"#008080\">{EMAIL[i]}</font>" if EMAIL[i] != "N/A" else ""
                    }
                },
                {
                    "decoratedText": {
                        "topLabel": f"<b>Timestamp</b>",
                        "text": f"<font color=\"#008000\">{date_str}</font> <font color=\"#008080\">{time_str}</font> (<font color=\"#808080\">{elapsed}</font>)",
                    }
                }
            ]
        }

        widgets.append(widget)

    payload = {
        "cardsV2": [
            {
                "card":{
                    "header": {
                        "title": f"{SERVER_NAME} Failed Builds",
                        "subtitle": f"Failed builds in {JOB_NAME}",
                        "imageUrl": "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSfi1Uk4jV99Qs8cse08amGsskhgChp4_qcQMMuFuhpW0brxv0Tw1bAQIS5BjL7MwgsnuY&usqp=CAU",
                    },
                    "sections": widgets + [
                        {
                            "widgets": [
                                {
                                    "columns": {
                                        "columnItems": [
                                            {
                                                "horizontalSizeStyle": "FILL_AVAILABLE_SPACE",
                                                "horizontalAlignment": "CENTER",
                                                "verticalAlignment": "CENTER",
                                                "widgets": [
                                                    {
                                                        "decoratedText": {
                                                            "text": f"Total Builds: <font color=\"#FF0000\"><b>{len(BUILD_NUMBER)}</b></font>"
                                                        }
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                }

                            ]
                        }
                    ],
                }
            }
        ]
    }

    headers = {"Content-Type": "application/json"}

    response = requests.post(WEBHOOK_URL, headers=headers, data=json.dumps(payload), verify=False)

    if response.status_code != 200:
        print(f"Error: {response.status_code}, {response.text}")

def pjdjenkins():
    global JOB_NAME
    global BUILD_NAME
    global BUILD_PATH
    global BUILD_NUMBER
    global BUILD_URL
    global BUILD_TIMESTAMP
    global OWNER
    global EMAIL

    JOBS = get_top_level_jobs()

    # Jobs List:
    '''
        "Build All Connections Pink", "BVT", "cnx-server", "CommonServices", "ComponentPack",
        "conn-automation", "Connections", "Dev", "devsubXpull", "Jenkins-demo-jitendra",
        "JenkinsBlue", "Sonar", "test_Copy_Kits_To_NonFed", "tmp-ComponentPack"
    '''
    # Exclude/Filter Jobs <add entry from Jobs list to filter>
    FILTER_JOBS = [
        "Build All Connections Pink",
        "CommonServices",
        "Dev",
        "Jenkins-demo-jitendra",
        "tmp-ComponentPack"
    ]

    for job in JOBS:
        JOB_NAME = job.replace("%20", " ")
        BUILD_NAME = []
        BUILD_PATH = []
        BUILD_NUMBER = []
        BUILD_URL = []
        BUILD_TIMESTAMP = []
        OWNER = []
        EMAIL = []

        if JOB_NAME in FILTER_JOBS:
            continue
        
        url = f"{SERVER_URL}/job/{job}/api/json?tree=fullDisplayName,lastBuild[*]"
        
        response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

        if response.status_code == 200:
            data = response.json()

            if data.get('_class') == "com.cloudbees.hudson.plugins.folder.Folder":                
                NEST_JOBS = folder_url(job)

                for nest_job in NEST_JOBS:
                    url = f"{SERVER_URL}/job/{job}/job/{nest_job}/api/json?tree=fullDisplayName,lastBuild[*]"

                    response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

                    if response.status_code == 200:
                        nest_data = response.json()

                        try: 
                            url = f"{SERVER_URL}/job/{job}/job/{nest_job}/{nest_data.get('lastBuild')['number']}/api/json?tree=actions[causes[*]]"
                            if nest_data.get('lastBuild')['result'] != "FAILURE":
                                get_build_details(nest_data, url)
                                send_message()
                        except TypeError:
                            if data.get('_class') == "com.cloudbees.hudson.plugins.folder.Folder":
                                INNER_NEST_JOBS = subfolder_url(job, nest_job)

                                for inner_nest_job in INNER_NEST_JOBS:
                                    url = f"{SERVER_URL}/job/{job}/job/{nest_job}/job/{inner_nest_job}/api/json?tree=fullDisplayName,lastBuild[*]"

                                    response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

                                    if response.status_code == 200:
                                        inner_nest_data = response.json()

                                        try: 
                                            url = f"{SERVER_URL}/job/{job}/job/{nest_job}/job/{inner_nest_job}/{inner_nest_data.get('lastBuild')['number']}/api/json?tree=actions[causes[*]]"

                                            if inner_nest_data.get('lastBuild')['result'] == "FAILURE":
                                                get_build_details(inner_nest_data, url)
                                                send_message()
                                        except TypeError:
                                            continue
                            continue

            if data.get('_class') == "org.jenkinsci.plugins.workflow.job.WorkflowJob":
                if data.get('lastBuild')['result'] == "FAILURE":
                    url = f"{SERVER_URL}/job/{job}/{data.get('lastBuild')['number']}/api/json?tree=actions[causes[*]]"
                    get_build_details(data, url)
                    send_message()

def folder_url(job):
    url = f"{SERVER_URL}/job/{job}/api/json??tree=jobs[*]"

    response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

    if response.status_code == 200:
        data = response.json()
        NEST_JOBS = []

        for nest_job in data.get('jobs', []):
            NEST_JOBS.append(nest_job['name'].replace(" ", "%20"))

        return NEST_JOBS

def subfolder_url(job, nest_job):
    url = f"{SERVER_URL}/job/{job}/job/{nest_job}/api/json??tree=jobs[*]"

    response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

    if response.status_code == 200:
        data = response.json()
        INNER_NEST_JOBS = []

        for inner_nest_job in data.get('jobs', []):
            INNER_NEST_JOBS.append(inner_nest_job['name'].replace(" ", "%20"))

        return INNER_NEST_JOBS

def get_build_details(data, url):
    BUILD_NAME.append(data.get('lastBuild')['displayName'])
    BUILD_PATH.append(data.get('lastBuild')['fullDisplayName'])
    BUILD_NUMBER.append(data.get('lastBuild')['number'])
    BUILD_URL.append(data.get('lastBuild')['url'])
    BUILD_TIMESTAMP.append(data.get('lastBuild')['timestamp'])

    response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

    if response.status_code == 200:
        data = response.json()

        for action in data.get('actions', []):
            causes = action.get('causes', [])

            for cause in causes:
                if cause.get('_class') == "hudson.model.Cause$UserIdCause":
                    OWNER.append(cause.get('userName').split('(', 1)[0])
                    EMAIL.append(cause.get('userId'))
                else:
                    OWNER.append("N/A")
                    EMAIL.append("N/A")

    return None

def get_top_level_jobs():
    url = f"{SERVER_URL}/api/json"

    response = requests.get(url, auth=(USER, API_TOKEN), verify=False)

    if response.status_code == 200:
        data = response.json()

        return [job['name'].replace(" ", "%20") for job in data['jobs']]

    return None

def convert_timestamp(timestamp_ms):
    timestamp_sec = timestamp_ms / 1000.0
    dt_object = datetime.fromtimestamp(timestamp_sec)
    date_str = dt_object.strftime('%m/%d/%Y')
    time_str = dt_object.strftime('%H:%M:%S')
    today = datetime.today()
    dt_difference = today - dt_object
    days = dt_difference.days
    hours, remainder = divmod(dt_difference.seconds, 3600)
    minutes, seconds = divmod(remainder, 60)

    if days > 1:
        elapsed = f"{days}d {hours}h {minutes}m {seconds}s ago"
    else:
        elapsed = f"{hours}h {minutes}m {seconds}s ago"
    
    return time_str, date_str, elapsed

if __name__ == "__main__":
    pjdjenkins()
