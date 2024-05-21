#!/bin/bash
server = "labgenserv.ibtec.ufu.br"
#server="bioinfo.facom.ufu.br"
email="medpipe.agent@gmail.com"

dirfastaFile=target.fasta # Do not alter this file name
cellWall="65" # Measure in amino acids
organismGroup="1" # 0=gram-negative, and 1=gram-positive bacteria
medpipePostURL="curl --location "$server"/v1/medpipe/run --form file=@$dirfastaFile --form cellWall=$cellWall --form organismGroup=$organismGroup --form email=$email"
echo "URL: $medpipePostURL"
processId=`$medpipePostURL`
echo "Result: $processId"

getStatusUrl="curl --location $server/v1/medpipe/$processId/status"
statusexec=`$getStatusUrl`
echo "status: $statusexec"

while [ $statusexec -gt 0 ]; do
    echo "Status: $statusexec"
    sleep 20
    getStatusUrl="curl --location $server/v1/medpipe/$processId/status"
    statusexec=`$getStatusUrl`
done        
echo "The microservice is done. Results:"

getUrl="curl --location $server/v1/medpipe/$processId/predictions 2>/dev/null"
Result=$(eval "$getUrl")
echo "MED stats:"
echo $Result

getUrl="curl --location $server/v1/medpipe/$processId/tmh 2>/dev/null"
Result=$(eval "$getUrl")
echo "TMH:"
echo $Result

getUrl="curl --location $server/v1/medpipe/$processId/signal 2>/dev/null"
Result=$(eval "$getUrl")
echo "SIGNAL:"
echo $Result
