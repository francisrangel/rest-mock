rest-mock
=========

A tiny test-framework to provide mock-responses for REST requests.
<br />



### Talk is cheap, show me the code!

  `Developer bob = new Developer("Bob", 25);`<br />
  
  `RestMock.whenGet("/developer/").thenReturnJSON(bob);`<br />
  `RestMock.startServer();`


*Ready!* 
Now you can access http://localhost:8080/developer/ and get the following JSON:

  `{ "name": "Bob", "age": 25 }`


*Alternatives:*
Do you preferer XML? What about:

  `RestMock.whenGet("/developer/").thenReturnXML(bob);`
  
*Ready!*
	`<?xml version=\"1.0\" ?>`
	`<developer>`
	  `<name>Bob</name>`
	  `<age>25</age>`
	`</developer>`
	

And more, you can define your return as a String using:
	a) `RestMock.whenGet("/developer/").thenReturnJSON("yourJSON");`
	b) `RestMock.whenGet("/developer/").thenReturnXML("yourXML");`
	c) `RestMock.whenGet("/developer/").thenReturnHtml("yourHTML");`
	d) `RestMock.whenGet("/developer/").thenReturnText("yourTxt");`

rest-mock will set the correct content type in http response.
