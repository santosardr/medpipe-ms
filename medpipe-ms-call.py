import requests
import time

server = "bioinfo.facom.ufu.br"
email = "medpipe.agent@gmail.com"
dirfastaFile = "target.fasta"
cellWall = "65"
organismGroup = "1"

# Post to the server
files = {'file': open(dirfastaFile, 'rb')}
data = {'cellWall': cellWall, 'organismGroup': organismGroup, 'email': email}
response = requests.post(f"http://{server}/v1/medpipe/run", files=files, data=data)
processId = response.text

# Get status
statusUrl = f"http://{server}/v1/medpipe/{processId}/status"
status = requests.get(statusUrl).text

# Wait for the process to finish
while int(status) > 0:
    print(f"Status: {status}")
    time.sleep(20)
    status = requests.get(statusUrl).text

# Get results
resultUrls = [f"http://{server}/v1/medpipe/{processId}/{result}" for result in ['predictions', 'tmh', 'signal']]
for resultUrl in resultUrls:
    result = requests.get(resultUrl).text
    print(f"{resultUrl.split('/')[-1].upper()}:")
    print(result)