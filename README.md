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


### Alternatives:

Do you preferer XML? What about:

  `RestMock.whenGet("/developer/").thenReturnXML(bob);`
  
*Ready!*<p>
	`<?xml version="1.0" ?>`<br />
	`<developer>`<br />
	`<name>Bob</name>`<br />
	`<age>25</age>`<br />
	`</developer>`<br /></p>
	

And more, you can define your return as a String using:<p>
	a) `RestMock.whenGet("/developer/").thenReturnJSON("yourJSON");`<br />
	b) `RestMock.whenGet("/developer/").thenReturnXML("yourXML");`<br />
	c) `RestMock.whenGet("/developer/").thenReturnHtml("yourHTML");`<br />
	d) `RestMock.whenGet("/developer/").thenReturnText("yourTxt");`</p>

rest-mock will set the correct content type in http response.
