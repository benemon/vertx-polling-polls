# Vert.x Polling Polls

This is a small project which retrieves GE2017 polling data from [OpinionBee's API](http://opinionbee.uk/api) and displays it in a really terrible JS UI.

### Configuration
It requires an OpinionBee API key which should be placed in the `OB_API_KEY` environment variable. 

The UI is hosted in the root context.

The API endpoints are:

* `/api/polls/{polling company}/{poll type}?[limit={sample size}]` - GET - where "polling company" is the company executing the poll (e.g. YouGov), and "poll type' is the type of poll being executed e.g. "Westminster voting intension". See OB API docs for details.


* `/api/companies` - GET - returns the list of Polling Companies

And...that's about it.

 
