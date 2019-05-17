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

//		System.out.println("\nStarting maintenance...");
		
		parent.stabilize();
		parent.fixFingers();
		parent.checkPredecessor();

//		System.out.println("Finished maintenance.");
		
	}
}
