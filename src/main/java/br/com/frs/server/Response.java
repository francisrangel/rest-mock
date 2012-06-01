package br.com.frs.server;

public interface Response {

	Response thenReturn(String desiredValue);
	Response withType(MediaType type);

}
