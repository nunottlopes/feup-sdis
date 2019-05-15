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
		System.out.println("\nStarting maintenance...");
		
		System.out.println("Predecessor = " + this.parent.predecessor);
		parent.stabilize();
		System.out.println("Predecessor = " + this.parent.predecessor);
		parent.fixFingers();
		System.out.println("Predecessor = " + this.parent.predecessor);
		parent.checkPredecessor();
		System.out.println("Predecessor = " + this.parent.predecessor);

		
		System.out.println("Finished maintenance.");
	}
}
