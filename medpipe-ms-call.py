import requests
import time


# List of available IP addresses or server names
servers = ["bioinfo.facom.ufu.br", "200.131.192.66",   "129.159.55.229"]


email = "medpipe.agent@gmail.com"
dirfastaFile = "target.fasta"
cellWall = "65"
organismGroup = "1"


def get_process_id(server):
    try:
        files = {'file': open(dirfastaFile, 'rb')}
        data = {'cellWall': cellWall, 'organismGroup': organismGroup, 'email': email}
        response = requests.post(f"http://{server}/v1/medpipe/run", files=files, data=data, timeout=20)
        response.raise_for_status()
        return response.text
    except requests.exceptions.Timeout:
        print(f"Timeout connecting to {server}")
        return None
    except requests.exceptions.RequestException as e:
        print(f"Error connecting to {server}: {e}")
        return None


def get_status(server, process_id):
    try:
        status_url = f"http://{server}/v1/medpipe/{process_id}/status"
        response = requests.get(status_url, timeout=20)
        response.raise_for_status()
        return int(response.text)
    except requests.exceptions.Timeout:
        print(f"Timeout getting status from {server}")
        return None
    except requests.exceptions.RequestException as e:
        print(f"Error getting status from {server}: {e}")
        return None


def get_results(server, process_id):
    try:
        result_urls = [f"http://{server}/v1/medpipe/{process_id}/{result}" for result in ['predictions', 'tmh', 'signal']]
        results = {}
        for result_url in result_urls:
            response = requests.get(result_url, timeout=20)
            response.raise_for_status()
            results[result_url.split('/')[-1].upper()] = response.text
        return results
    except requests.exceptions.Timeout:
        print(f"Timeout getting results from {server}")
        return None
    except requests.exceptions.RequestException as e:
        print(f"Error getting results from {server}: {e}")
        return None


def main():
    process_id = None
    current_server = None
    for server in servers:
        process_id = get_process_id(server)
        if process_id:
            current_server = server
            break
    if not process_id:
        print("No server responded successfully. Exiting.")
        exit(1)


    while True:
        status = get_status(current_server, process_id)
        if status is None:
            print(f"Error getting status from {current_server}. Tentando outro servidor...")
            for s in servers:
                if s != current_server:
                    status = get_status(s, process_id)
                    if status is not None:
                        current_server = s
                        break
            if status is None:
                print("No server responded successfully. Exiting.")
                exit(1)
        if status <= 0:
            break
        print(f"Status: {status}")
        time.sleep(20)


    results = get_results(current_server, process_id)
    if results:
        for key, value in results.items():
            print(f"{key}:")
            print(value)
    else:
        print("Failed to retrieve results.")


if __name__ == "__main__":
    main()
