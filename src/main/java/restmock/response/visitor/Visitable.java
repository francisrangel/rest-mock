package restmock.response.visitor;

public interface Visitable<E> {
	
	void accept(Visitor<E> visitor);

}
