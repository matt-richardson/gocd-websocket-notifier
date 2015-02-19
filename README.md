# gocd-websocket-notifier
WebSocket based GoCD build notifier

*Note*: this plugin is still under development. It should work for basic use cases.
There is a very high chance that the json that it publishes will change (to include more pipeline details).
If you run into any issues please [raise it](https://github.com/matt-richardson/gocd-websocket-notifier/issues),
or better yet, send a PR.

## Setup
Download jar from releases & place it in /plugins/external & restart Go Server.

## Configuration
Plugin listens on port 8887 by default.
To edit this, create a (standard java properties) file with the name
gocd-websocket-notifier.conf in the home directory of the user that runs go.
````
port=8888
````

## Planned Enhancements
* Send [a reply](http://www.go.cd/documentation/developer/writing_go_plugins/notification/version_1_0/stage_status_notification.html#response---from-the-plugin)
if cannot notify listeners.
* Query to get more [pipeline details](https://github.com/gocd/documentation/blob/master/user/api/pipeline_api.md#pipeline-history)
and include them in the published message. Unfortunately will require credentials configured in the config.

## License
http://www.apache.org/licenses/LICENSE-2.0

## Credits
Originally based on the [gocd-slack-notifier](http://github.com/ashwanthkumar/gocd-slack-notifer) by @AshwanthKumar.
