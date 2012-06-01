rest-mock
=========

A tiny test-framework to provide mock-responses for REST requests.
<br />



### Talk is cheap, show me the code!


  `String data = "{ \"name\": \"Bob\", \"age\": \"25\" }";`<br />
  `RestServer restServer = new RestServer(8080);`
  
  `restServer.when("/developer").thenReturn(new JSON(data));`<br />
  `restServer.start();`


*Ready!* 
Now you can access http://localhost:8080/developer/