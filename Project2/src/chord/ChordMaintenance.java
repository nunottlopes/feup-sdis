package chord;

public class ChordMaintenance implements Runnable
{
	Chord parent = null;
	
	public ChordMaintenance(Chord parent)
	{
		this.parent = parent;
	}
	
	@Override
	public void run()
	{
		
		System.out.println(parent);
		
		parent.stabilize();
		parent.fixFingers();
		parent.checkPredecessor();
		
	}
}
