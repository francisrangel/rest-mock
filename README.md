rest-mock
=========

A tiny framework to provide mock-responses for REST requests.


Talk is cheap, show me the code!
=========

String myJSONData = "{ \"name\": \"Bob\", \"age\": \"25\" }";
RestServer restServer = new RestServer(8080);

restServer.when("/developer").thenReturn(simpleJSON).withType(MediaType.APPLICATION_JSON);
restServer.start();

Ready! Now you can access http://localhost:8080/developer