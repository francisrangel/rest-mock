package restmock.response.visitor;

public interface Visitor<E> {

	void visit(E element);
	
}
