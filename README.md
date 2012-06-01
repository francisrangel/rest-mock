rest-mock
=========

A tiny test-framework to provide mock-responses for REST requests.
<br />



### Talk is cheap, show me the code!


  `String simpleJSON = "{ \"name\": \"Bob\", \"age\": \"25\" }";`<br />
  `RestServer restServer = new RestServer(8080);`
  
  `restServer.when("/developer").thenReturn(simpleJSON).withType(MediaType.APPLICATION_JSON);`<br />
  `restServer.start();`


*Ready!* 
Now you can access http://localhost:8080/developer/