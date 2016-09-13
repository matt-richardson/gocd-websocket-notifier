docker rm GoCD -f

mkdir -p ./docker-testing/lib/plugins/external
cp target/gocd-websocket-notifier*.jar ./docker-testing/lib/plugins/external/

docker run --name GoCD \
           --interactive \
           --tty \
           --publish-all \
           --publish 8887:8887 \
           --volume $PWD/docker-testing/lib:/var/lib/go-server \
           --volume $PWD/docker-testing/log:/var/log/go-server \
           gocd/gocd-server:latest

# .... wait ....
# echo http://localhost:$(docker inspect --format='{{(index (index .NetworkSettings.Ports "8153/tcp") 0).HostPort}}' GoCD)

