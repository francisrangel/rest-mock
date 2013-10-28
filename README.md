rest-mock
=========

A tiny test-framework to provide stub-responses for REST calls.
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

### External files responses

Well, sometimes you have complex answers. So, you don't want create an Object.
And paste all the file inside a String doesn't look good as well.

Ok my friend. What about save the files inside your resources test folder and keep your code clean?

Just use the fromResource methods:<p>
	a) `RestMock.whenGet("/reallyComplexEnterpriseCall/").thenReturnXMLFromResource("enterprise-answer.xml");`<br />
	b) `RestMock.whenGet("/hugeHTML/").thenReturnHtml("my-super-index.html");`</p>


### Testing error handling

Want to test a Forbidden error request?

	RestMock.whenGet("/developer/").thenReturnErrorCodeWithMessage(HttpServletResponse.SC_FORBIDDEN, "Forbidden GET");
	
When you send a GET request do this address you get:

	Forbidden GET
	
... and a HTTP 403 status.

You can use any HTTP status to mock different behaviour.