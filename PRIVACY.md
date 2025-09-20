# Cardinal Maps Privacy Policy

Cardinal Maps is an open-soure mapping application focused on helping you get around while respecting your privacy.

We do not collect your location data directly, but we do collect data in certain circumstances that is related to user location, or can be used to infer user location with some nonzero probability.

## Offline Mode

First things first, if you use Cardinal Maps in offline mode, the only data we collect is which areas of the earth you choose to download maps for, with one exception. We don't have analytics, don't show or target ads, or any do of the other invasive things that most other apps do. If you use the app offline, your location data never leaves your device.

The exception to this rule is when you have the "fetch transit information even while offline" setting enabled. This setting is enabled by default, but can be diabled easily in the Offline Settings screen, which is accessible by tapping the Cardinal icon in the top left of the screen and tapping "Offline Settings".

## Online Mode

In order to provide an online mode, we do collect some information, but we've limited it to the information that we absolutely must have in order to provide the online service in question.

### Configurable backends

Cardinal Maps allows you to configure the application so that you can choose to send search and routing requests to whomever you choose. If you decide that you trust e.g. Stadia Maps more than you trust maps.earth (the default) you can configure the app to send your search and routing requests to them instead. You can also point the app at a self-hosted service if you decide you only trust yourself. Once you modify these advanced settings, the information described in the sections below will be sent to whoever it is that you choose, rather than to us.

#### Online search

We collect your search queries in online mode. We need this information in order to provide you search results. If you search for "Paris, France", we need that raw text in order to look up where Paris is in our database.

#### Online routing

We collect the "to" and "from" locations on your routing requests. If you choose to give the app location permissions, the "from" endpoint for your online routing requests will sometimes be your current location, and that location will be sent to our servers. If you do not choose to give the app location permissions, you can fetch routes for whichever locations you choose. We have no way of knowing whether the route requests we receive correspond to a user's location or not. This information is processed ephemerally and kept in our logs that are typically kept for about a week. We don't have any log persistence set up, but neither do we make any concerted effort to wipe our logs at fixed intervals. If you would like this feature, please help us out! Our default backend, maps.earth, is powered by Headway, an open-source web maps stack. We'd love an audit to make sure our logs aren't being retained for any longer than strictly necessary.
