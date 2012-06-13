rest-mock
=========

A tiny test-framework to provide mock-responses for REST requests.
<br />



### Talk is cheap, show me the code!

  `Developer bob = new Developer("Bob", 25);`<br />
  
  `RestMock.whenGet("/developer/").thenReturn(new JSON(bob));`<br />
  `RestMock.startServer();`


*Ready!* 
Now you can access http://localhost:8080/developer/ and get the following JSON:

  `{ "name": "Bob", "age": 25 }`
