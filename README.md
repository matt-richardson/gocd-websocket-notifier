# gocd-websocket-notifier

[![Build Status](https://travis-ci.org/matt-richardson/gocd-websocket-notifier.svg?branch=master)](https://travis-ci.org/matt-richardson/gocd-websocket-notifier)

WebSocket based GoCD build notifier

Listens to notifications from the GoCD (15.2+) notification api, and publishes them over websockets.
This initial use case is for low impact, fast build notifiers, such as
[gocd-windows-tray-build-notifier](https://github.com/matt-richardson/gocd-windows-tray-build-notifier).

The only supported notification (the only one available as of Feb 2015) is the [stage-status](http://www.go.cd/documentation/developer/writing_go_plugins/notification/version_1_0/stage_status_notification.html).
This has been enhanced to provide further pipeline instance information (via the [pipeline
history api](http://www.go.cd/documentation/user/current/api/pipeline_api.html#pipeline-history) and 
includes the latest run in the message as `x-pipeline-instance-details`.

If you run into any issues please [raise it](https://github.com/matt-richardson/gocd-websocket-notifier/issues),
or better yet, send a PR. Please note, I am not a Java developer, so any (constructive) feedback gratefully
appreciated.

## Setup
Download jar from releases & place it in /plugins/external & restart Go Server.

## Configuration
Plugin listens on port 8887 on all ip addresses by default.
Plugin uses default Go server HTTP port 8153.
To edit these, create a (standard java properties) file with the name
gocd-websocket-notifier.conf in the home directory of the user that runs go.
````
port=8888
host=127.0.0.1
goHttpPort=8080
````
If basic authenication is required for Go Api, add them to the same .conf file as follows:
````
goUser=go.user
goPassword=myGoPassword
````

## Planned Enhancements
* Send [a reply](http://www.go.cd/documentation/developer/writing_go_plugins/notification/version_1_0/stage_status_notification.html#response---from-the-plugin)
if cannot notify listeners.

## License
http://www.apache.org/licenses/LICENSE-2.0

## Credits
Originally based on the [gocd-slack-build-notifier](https://github.com/ashwanthkumar/gocd-slack-build-notifier) by @AshwanthKumar.
