rest-mock
=========

A tiny test-framework to provide stub-responses for REST calls.
<br />


### Talk is cheap, show me the code! 
``` java
Developer bob = new Developer("Bob", 25);
  
RestMock.whenGet("/developer/").thenReturnJSON(bob);
RestMock.startServer();
```


*Ready!* 
Now you can access http://localhost:8080/developer/ and get the following JSON:
``` json 
{ "name": "Bob", "age": 25 }
```

### Alternatives:

Do you preferer XML? What about:  
``` java
RestMock.whenGet("/developer/").thenReturnXML(bob);
```
  
*Ready!*
``` xml
	<?xml version="1.0" ?>
	<developer>
	<name>Bob</name>
	<age>25</age>
	</developer>
```	

And more, you can define your return as a literal using:
``` java
RestMock.whenGet("/developer/").thenReturnJSON("yourJSON");
RestMock.whenGet("/developer/").thenReturnXML("yourXML");
RestMock.whenGet("/developer/").thenReturnHtml("yourHTML");
RestMock.whenGet("/developer/").thenReturnText("yourTxt");
``` 

rest-mock will set the correct content type in http response.

### External files responses

Well, sometimes you have complex answers. So, you don't want create an Object.
And paste all the file inside a String doesn't look good as well.

Ok my friend. What about save the files inside your resources test folder and keep your code clean?

Just use the fromResource methods:
``` java
RestMock.whenGet("/reallyComplexEnterprise/").thenReturnXMLFromResource("enterprise-answer.xml");
RestMock.whenGet("/hugeHTML/").thenReturnHtml("my-super-index.html");
```

### Testing error handling

Want to test a Forbidden error request?
``` java
RestMock.whenGet("/developer/")
	.thenReturnErrorCodeWithMessage(HttpServletResponse.SC_FORBIDDEN, "Forbidden GET");
```
	
When you send a GET request do this address you get:

	Forbidden GET
	
... and a HTTP 403 status.

You can use any HTTP status to mock different behaviour.

### Dynamic Response

Sometimes you expect a result based on your request parameters.
For those purpose you can use the wildtag ${parameter}.

``` java
RestMock.whenGet("/hello").thenReturnText("Hello ${name}!");
RestMock.startServer();
``` 

When you access /test?name=Bob, you got: 
``` text
Hello Bob!
```

### Stub Headers
Do you need some data in your header? Don't worry. 
``` java
RestMock.whenGet("/hello").thenReturnText("Hello ${name}!").withHeader("Cache-Control", "no-cache");
RestMock.whenGet("/hi").thenReturnText("Hi").withHeader("a", "1").withHeader("b", "2").withHeader("c", "3");
``` 
