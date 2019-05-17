package global;

public class Pair<A,B>
{
	public A first;
	public B second;
	
	public Pair(A a, B b)
	{
		this.first = a;
		this.second = b;
	}
	
	public Pair(Pair<A, B> pair)
	{
		this.first = pair.first;
		this.second = pair.second;
	}

	@Override
	public String toString()
	{
		return "{" + first + ", " + second + "}";
	}
	
	
}