rest-mock
=========

A tiny test-framework to provide mock-responses for REST requests.
<br />



### Talk is cheap, show me the code!

  `Developer bob = new Developer("Bob", 25);`<br />
  `RestServer restServer = new RestServer(8080);`
  
  `restServer.whenGet("/developer/").thenReturn(new JSON(bob));`<br />
  `restServer.start();`


*Ready!* 
Now you can access http://localhost:8080/developer/ and get the following JSON:

  `{ "name": "Bob", "age": 25 }`
